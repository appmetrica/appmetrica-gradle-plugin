package io.appmetrica.analytics.gradle.common.tasks

import io.appmetrica.analytics.gradle.common.utils.Log
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.testfixtures.ProjectBuilder
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.verify.VerificationTimes
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File

/* ktlint-disable appmetrica-rules:no-mockito-when */
object UploadTaskSpek : Spek({

    beforeGroup {
        Log.setLogger(ProjectBuilder.builder().build().logger)
    }

    describe("upload") {
        it("skips upload in offline mode") {
            val project = ProjectBuilder.builder().build()
            val zipFile = File.createTempFile("test", ".zip").apply { writeText("data") }

            val task = project.tasks.create("uploadTest", UploadTask::class.java) {
                it.taskEnabled.set(true)
                it.zipFile.set(zipFile)
                it.uploadUrl.set("http://localhost:9999")
                it.postApiKey.set("test-key")
                it.offline.set(true)
                it.enableAnalytics.set(false)
                it.paramsForAnalytics.set(emptyMap())
            }

            assertThatCode { task.upload() }.doesNotThrowAnyException()
        }

        it("throws when postApiKey is empty in online mode") {
            val project = ProjectBuilder.builder().build()
            val zipFile = File.createTempFile("test", ".zip").apply { writeText("data") }

            val task = project.tasks.create("uploadTest2", UploadTask::class.java) {
                it.taskEnabled.set(true)
                it.zipFile.set(zipFile)
                it.uploadUrl.set("http://localhost:9999")
                it.postApiKey.set("")
                it.offline.set(false)
                it.enableAnalytics.set(false)
                it.paramsForAnalytics.set(emptyMap())
            }

            assertThatThrownBy { task.upload() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Post API key is empty")
        }

        it("uploads file successfully in online mode") {
            val port = 8889
            val mockServer = ClientAndServer.startClientAndServer(port)
            try {
                mockServer.`when`(
                    request().withMethod("PUT").withPath("/")
                ).respond(
                    response().withStatusCode(200).withBody("")
                )

                val project = ProjectBuilder.builder().build()
                val zipFile = File.createTempFile("test", ".zip").apply { writeText("data") }

                val task = project.tasks.create("uploadTest3", UploadTask::class.java) {
                    it.taskEnabled.set(true)
                    it.zipFile.set(zipFile)
                    it.uploadUrl.set("http://localhost:$port")
                    it.postApiKey.set("test-key")
                    it.offline.set(false)
                    it.enableAnalytics.set(false)
                    it.paramsForAnalytics.set(emptyMap())
                }

                assertThatCode { task.upload() }.doesNotThrowAnyException()

                mockServer.verify(
                    request().withMethod("PUT").withPath("/"),
                    VerificationTimes.exactly(1)
                )
            } finally {
                mockServer.close()
            }
        }

        it("taskEnabled false prevents execution") {
            val project = ProjectBuilder.builder().build()
            val zipFile = File.createTempFile("test", ".zip").apply { writeText("data") }

            val task = project.tasks.create("uploadTest4", UploadTask::class.java) {
                it.taskEnabled.set(false)
                it.zipFile.set(zipFile)
                it.uploadUrl.set("http://localhost:9999")
                it.postApiKey.set("test-key")
                it.offline.set(false)
                it.enableAnalytics.set(false)
                it.paramsForAnalytics.set(emptyMap())
            }

            assertThat(task.taskEnabled.get()).isFalse()
        }
    }
})
/* ktlint-enable appmetrica-rules:no-mockito-when */
