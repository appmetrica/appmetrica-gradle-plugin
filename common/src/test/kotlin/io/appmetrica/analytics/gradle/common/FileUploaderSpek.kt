package io.appmetrica.analytics.gradle.common

import org.apache.http.client.HttpResponseException
import org.assertj.core.api.Assertions.assertThat
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.Header.header
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.verify.VerificationTimes
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File

/* ktlint-disable appmetrica-rules:no-mockito-when */
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
            mockServer.`when`(
                request()
                    .withMethod("PUT")
                    .withPath("/")

            ).respond(
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

        it("retries if error 500") {
            mockServer.`when`(
                request()
                    .withMethod("PUT")
                    .withPath("/")

            ).respond(
                response()
                    .withStatusCode(500)
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
                VerificationTimes.exactly(MAX_RETRY_COUNT)
            )
        }

        it("throws exception if not error 500") {
            mockServer.`when`(
                request()
                    .withMethod("PUT")
                    .withPath("/")

            ).respond(
                response()
                    .withStatusCode(404)
                    .withBody("")
            )

            try {
                fileUploader.uploadFile(File.createTempFile("prefix", "postfix"))
                assertThat(true).isFalse()
            } catch (e: HttpResponseException) {
                assertThat(e.statusCode).isEqualTo(404)
            }
        }

        it("succeeds after intermediate 500 responses") {
            mockServer.`when`(
                request().withMethod("PUT").withPath("/"),
                Times.exactly(2)
            ).respond(
                response().withStatusCode(500).withBody("")
            )
            mockServer.`when`(
                request().withMethod("PUT").withPath("/")
            ).respond(
                response().withStatusCode(200).withBody("")
            )

            fileUploader.uploadFile(File.createTempFile("prefix", "postfix"))

            mockServer.verify(
                request().withMethod("PUT").withPath("/"),
                VerificationTimes.exactly(MAX_RETRY_COUNT)
            )
        }

        it("retry budget is independent across calls on the same instance") {
            mockServer.`when`(
                request().withMethod("PUT").withPath("/")
            ).respond(
                response().withStatusCode(500).withBody("")
            )

            fileUploader.uploadFile(File.createTempFile("prefix1", "postfix"))
            fileUploader.uploadFile(File.createTempFile("prefix2", "postfix"))

            mockServer.verify(
                request().withMethod("PUT").withPath("/"),
                VerificationTimes.exactly(MAX_RETRY_COUNT * 2)
            )
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
            mockServer.`when`(
                request().withMethod("PUT")
            ).respond(
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
/* ktlint-enable appmetrica-rules:no-mockito-when */
