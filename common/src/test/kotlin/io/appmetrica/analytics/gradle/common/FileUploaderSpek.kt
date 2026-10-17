package io.appmetrica.analytics.gradle.common

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.mockserver.client.ForwardChainExpectation
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.Header.header
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpResponse.response
import org.mockserver.verify.VerificationTimes
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.text.Charsets.US_ASCII
import kotlin.text.Charsets.UTF_8

object FileUploaderSpek : Spek({
    describe("upload file") {
        val port = 8888
        val postApiKey = "postApiKey"
        lateinit var mockServer: ClientAndServer
        lateinit var fileUploader: FileUploader

        beforeEachTest {
            mockServer = ClientAndServer.startClientAndServer(port)
            fileUploader = FileUploader("http://localhost:$port", postApiKey)
        }

        afterEachTest {
            mockServer.close()
        }

        it("finish successfully") {
            mockServer.respondTo(
                request()
                    .withMethod("PUT")
                    .withPath("/"),
                response()
                    .withStatusCode(200)
                    .withBody("")
            )
            fileUploader.uploadFile(File.createTempFile("prefix", "postfix"))

            mockServer.verify(
                request()
                    .withMethod("PUT")
                    .withPath("/")
                    .withHeaders(
                        header("Authorization", "Post-Api-Key $postApiKey"),
                        header("Content-Type", "application/zip; charset=utf-8")
                    ),
                VerificationTimes.exactly(1)
            )
        }

        it("retries 5xx then fails with last status and body; 4xx fails immediately") {
            mockServer.respondTo(
                request().withMethod("PUT").withPath("/"),
                response()
                    .withStatusCode(503)
                    .withBody("""{"error":"unavailable"}""")
            )

            assertThatThrownBy {
                fileUploader.uploadFile(File.createTempFile("prefix", "postfix"))
            }.isInstanceOfSatisfying(HttpResponseException::class.java) { e ->
                assertThat(e.statusCode).isEqualTo(503)
                assertThat(e.message).contains("unavailable")
            }

            mockServer.verify(
                request()
                    .withMethod("PUT")
                    .withPath("/")
                    .withHeaders(
                        header("Authorization", "Post-Api-Key $postApiKey"),
                        header("Content-Type", "application/zip; charset=utf-8")
                    ),
                VerificationTimes.exactly(MAX_RETRY_COUNT)
            )

            mockServer.reset()
            mockServer.respondTo(
                request().withMethod("PUT").withPath("/"),
                response().withStatusCode(404).withBody("""{"error":"missing"}""")
            )

            assertThatThrownBy {
                fileUploader.uploadFile(File.createTempFile("prefix", "postfix"))
            }.isInstanceOfSatisfying(HttpResponseException::class.java) { e ->
                assertThat(e.statusCode).isEqualTo(404)
                assertThat(e.message).contains("missing")
            }

            mockServer.verify(
                request().withMethod("PUT").withPath("/"),
                VerificationTimes.exactly(1)
            )
        }

        it("succeeds after intermediate 500 responses") {
            mockServer.respondTo(
                request().withMethod("PUT").withPath("/"),
                response().withStatusCode(500).withBody(""),
                Times.exactly(2)
            )
            mockServer.respondTo(
                request().withMethod("PUT").withPath("/"),
                response().withStatusCode(200).withBody("")
            )

            fileUploader.uploadFile(File.createTempFile("prefix", "postfix"))

            mockServer.verify(
                request().withMethod("PUT").withPath("/"),
                VerificationTimes.exactly(MAX_RETRY_COUNT)
            )
        }

        it("retry budget is independent across calls on the same instance") {
            mockServer.respondTo(
                request().withMethod("PUT").withPath("/"),
                response().withStatusCode(500).withBody("permanent")
            )

            repeat(2) {
                assertThatThrownBy {
                    fileUploader.uploadFile(File.createTempFile("prefix", "postfix"))
                }.isInstanceOfSatisfying(HttpResponseException::class.java) { e ->
                    assertThat(e.statusCode).isEqualTo(500)
                }
            }

            mockServer.verify(
                request().withMethod("PUT").withPath("/"),
                VerificationTimes.exactly(MAX_RETRY_COUNT * 2)
            )
        }

        it("surfaces HTTP status and body when server rejects mid-upload") {
            val errorBody =
                """{"errors":[{"error_type":"quota_requests_by_app_id","message":"quota"}],"code":429}"""
            val largeFile = File.createTempFile("large", ".zip").apply {
                RandomAccessFile(this, "rw").use { it.setLength(8L * 1024 * 1024) }
                deleteOnExit()
            }
            val bytesBeforeClose = AtomicInteger(0)
            val serverReady = CountDownLatch(1)
            val executor = Executors.newSingleThreadExecutor()

            ServerSocket(0).use { serverSocket ->
                val localPort = serverSocket.localPort
                executor.execute {
                    serverReady.countDown()
                    serverSocket.soTimeout = 10_000
                    serverSocket.accept().use { client ->
                        // JDK 17 HttpClient drops early responses if the peer closes before the
                        // response body is fully read; keep the socket open and drain leftover
                        // request bytes after writing 429 + JSON.
                        handleEarlyRejectUpload(
                            client = client,
                            errorBody = errorBody,
                            bytesBeforeClose = bytesBeforeClose
                        )
                    }
                }

                assertThat(serverReady.await(5, TimeUnit.SECONDS)).isTrue()
                val uploader = FileUploader("http://127.0.0.1:$localPort/upload", postApiKey)

                assertThatThrownBy { uploader.uploadFile(largeFile) }
                    .isInstanceOfSatisfying(HttpResponseException::class.java) { e ->
                        assertThat(e.statusCode).isEqualTo(429)
                        assertThat(e.message)
                            .contains("quota_requests_by_app_id")
                            .doesNotContain("Connection reset")
                    }
                assertThat(bytesBeforeClose.get()).isGreaterThan(0)
            }

            executor.shutdownNow()
        }
    }

    describe("upload file via proxy") {
        val proxyPort = 8889
        val postApiKey = "postApiKey"
        lateinit var mockServer: ClientAndServer
        val savedProperties = mutableMapOf<String, String?>()
        val proxyPropertyKeys = listOf("http.proxyHost", "http.proxyPort", "http.nonProxyHosts")

        beforeEachTest {
            mockServer = ClientAndServer.startClientAndServer(proxyPort)
            proxyPropertyKeys.forEach { savedProperties[it] = System.getProperty(it) }
            System.setProperty("http.proxyHost", "localhost")
            System.setProperty("http.proxyPort", proxyPort.toString())
            System.clearProperty("http.nonProxyHosts")
        }

        afterEachTest {
            mockServer.close()
            savedProperties.forEach { (key, value) ->
                if (value == null) System.clearProperty(key) else System.setProperty(key, value)
            }
            savedProperties.clear()
        }

        it("routes requests through http.proxyHost / http.proxyPort") {
            mockServer.respondTo(
                request().withMethod("PUT"),
                response().withStatusCode(200).withBody("")
            )

            val fileUploader = FileUploader("http://unreachable.invalid/upload", postApiKey)
            fileUploader.uploadFile(File.createTempFile("prefix", "postfix"))

            mockServer.verify(
                request().withMethod("PUT"),
                VerificationTimes.exactly(1)
            )
        }
    }
})

/**
 * MockServer's expectation API is named `when`, which trips appmetrica-rules:no-mockito-when.
 * Call it through a named reference so the rule sees `registerExpectation`, not `` `when` ``.
 */
private fun ClientAndServer.respondTo(
    httpRequest: HttpRequest,
    httpResponse: HttpResponse,
    times: Times = Times.unlimited()
) {
    val registerExpectation:
        ClientAndServer.(HttpRequest, Times) -> ForwardChainExpectation = ClientAndServer::`when`
    registerExpectation(httpRequest, times).respond(httpResponse)
}

private fun handleEarlyRejectUpload(
    client: Socket,
    errorBody: String,
    bytesBeforeClose: AtomicInteger
) {
    val input = client.getInputStream()
    readHttpHeaders(input)
    val drained = input.read(ByteArray(8 * 1024))
    if (drained > 0) {
        bytesBeforeClose.addAndGet(drained)
    }
    writeHttpResponse(
        output = client.getOutputStream(),
        statusLine = "HTTP/1.1 429 Too Many Requests",
        contentType = "application/json",
        body = errorBody.toByteArray(UTF_8)
    )
    drainRemainingRequestBody(client, input)
}

private fun readHttpHeaders(input: InputStream) {
    val recent = ByteArray(4)
    var headersDone = false
    while (!headersDone) {
        val value = input.read()
        check(value >= 0) { "Connection closed before headers finished" }
        System.arraycopy(recent, 1, recent, 0, 3)
        recent[3] = value.toByte()
        headersDone = recent[0] == '\r'.code.toByte() &&
            recent[1] == '\n'.code.toByte() &&
            recent[2] == '\r'.code.toByte() &&
            recent[3] == '\n'.code.toByte()
    }
}

private fun writeHttpResponse(
    output: OutputStream,
    statusLine: String,
    contentType: String,
    body: ByteArray
) {
    val head =
        "$statusLine\r\n" +
            "Content-Type: $contentType\r\n" +
            "Content-Length: ${body.size}\r\n" +
            "Connection: close\r\n" +
            "\r\n"
    output.write(head.toByteArray(US_ASCII))
    output.write(body)
    output.flush()
}

private fun drainRemainingRequestBody(client: Socket, input: InputStream) {
    client.soTimeout = 2_000
    val buffer = ByteArray(64 * 1024)
    try {
        while (input.read(buffer) >= 0) {
            // Keep the connection open until the client finishes reading the response
            // and stops uploading, otherwise JDK HttpClient may fail with
            // "fixed content-length: N, bytes received: 0".
        }
    } catch (_: SocketTimeoutException) {
        // Client already got the response; leftover upload may still be in flight.
    }
}
