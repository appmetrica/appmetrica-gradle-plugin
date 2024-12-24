package io.appmetrica.analytics.gradle.common.tasks

import io.appmetrica.analytics.gradle.common.FileUploader
import io.appmetrica.analytics.gradle.common.Log
import io.appmetrica.analytics.gradle.common.UPLOAD_FAILED_TEMPLATE
import io.appmetrica.analytics.gradle.common.analytics.Analytics
import io.appmetrica.analytics.gradle.common.analytics.AnalyticsReporter
import io.appmetrica.analytics.gradle.common.analytics.AnalyticsStub
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import kotlin.math.abs

@UntrackedTask(because = "Make task cache is in progress")
abstract class UploadTask : DefaultTask() {

    @get:InputFile
    abstract val zipFile: RegularFileProperty

    @Internal
    lateinit var postApiKey: String

    @Internal
    lateinit var uploadUrl: String

    @Internal
    var offline: Boolean = false

    @Internal
    var enableAnalytics: Boolean = true

    @Internal
    var paramsForAnalytics: Map<String, Any> = emptyMap()

    private val analytics: Analytics by lazy {
        if (enableAnalytics) {
            AnalyticsReporter(getUserId(postApiKey))
        } else {
            AnalyticsStub()
        }
    }

    @Suppress("TooGenericExceptionThrown")
    @TaskAction
    fun upload() {
        analytics.reportEvent(
            "upload",
            params = paramsForAnalytics
        )
        if (offline) {
            Log.warn(UPLOAD_FAILED_TEMPLATE.format("Offline mode enabled", zipFile.get().asFile.absolutePath))
        } else {
            checkParameters()
            val fileUploader = FileUploader(uploadUrl, postApiKey)
            try {
                fileUploader.uploadFile(zipFile.get().asFile)
                Log.info("File ${zipFile.get().asFile.name} successfully uploaded.")
            } catch (e: Throwable) {
                Log.error(UPLOAD_FAILED_TEMPLATE.format(e.message, zipFile.get().asFile.absolutePath))
                analytics.reportError(
                    e.message ?: e::class.java.simpleName,
                    throwable = e,
                    params = paramsForAnalytics
                )
                throw e
            }
        }
    }

    private fun checkParameters() {
        if (postApiKey.isEmpty()) {
            IllegalArgumentException("Post API key is empty for task $name.").also {
                analytics.reportError(
                    "Post API key is empty",
                    throwable = it,
                    params = paramsForAnalytics
                )
                throw it
            }
        }
    }

    private fun getUserId(postApiKey: String) = abs(postApiKey.hashCode()).toString()
}
