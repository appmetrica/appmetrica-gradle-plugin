package io.appmetrica.analytics.gradle.common

import org.apache.http.client.HttpResponseException
import org.assertj.core.api.Assertions.assertThat
import org.mockserver.integration.ClientAndServer
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
                VerificationTimes.exactly(3)
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
    }
})
/* ktlint-enable appmetrica-rules:no-mockito-when */
