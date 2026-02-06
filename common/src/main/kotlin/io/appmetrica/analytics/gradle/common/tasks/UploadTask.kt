package io.appmetrica.analytics.gradle.common.tasks

import io.appmetrica.analytics.gradle.common.FileUploader
import io.appmetrica.analytics.gradle.common.Log
import io.appmetrica.analytics.gradle.common.UPLOAD_FAILED_TEMPLATE
import io.appmetrica.analytics.gradle.common.analytics.Analytics
import io.appmetrica.analytics.gradle.common.analytics.AnalyticsReporter
import io.appmetrica.analytics.gradle.common.analytics.AnalyticsStub
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import kotlin.io.path.Path
import kotlin.io.path.fileSize
import kotlin.math.abs

@UntrackedTask(because = "Make task cache is in progress")
abstract class UploadTask : DefaultTask() {

    @get:InputFile
    abstract val zipFile: RegularFileProperty

    @get:Internal
    abstract val postApiKey: Property<String>

    @get:Internal
    abstract val uploadUrl: Property<String>

    @get:Internal
    abstract val offline: Property<Boolean>

    @get:Internal
    abstract val enableAnalytics: Property<Boolean>

    @get:Internal
    abstract val paramsForAnalytics: MapProperty<String, Any>

    @Suppress("TooGenericExceptionThrown", "TooGenericExceptionCaught")
    @TaskAction
    fun upload() {
        val analytics = createAnalytics()
        val params = paramsForAnalytics.get()

        Log.info(
            "Uploading file ${zipFile.get().asFile.name} with size " +
                "${Path(zipFile.get().asFile.absolutePath).fileSize()}"
        )
        analytics.reportEvent(
            "upload",
            params = params
        )
        if (offline.get()) {
            Log.warn(UPLOAD_FAILED_TEMPLATE.format("Offline mode enabled", zipFile.get().asFile.absolutePath))
        } else {
            checkParameters(analytics, params)
            val fileUploader = FileUploader(uploadUrl.get(), postApiKey.get())
            try {
                fileUploader.uploadFile(zipFile.get().asFile)
                Log.info("File ${zipFile.get().asFile.name} successfully uploaded.")
            } catch (e: Throwable) {
                Log.error(UPLOAD_FAILED_TEMPLATE.format(e.message, zipFile.get().asFile.absolutePath))
                analytics.reportError(
                    e.message ?: e::class.java.simpleName,
                    throwable = e,
                    params = params
                )
                throw e
            }
        }
    }

    private fun createAnalytics(): Analytics {
        return if (enableAnalytics.get()) {
            AnalyticsReporter(getUserId(postApiKey.get()))
        } else {
            AnalyticsStub()
        }
    }

    private fun checkParameters(analytics: Analytics, params: Map<String, Any>) {
        if (postApiKey.get().isEmpty()) {
            IllegalArgumentException("Post API key is empty for task $name.").also {
                analytics.reportError(
                    "Post API key is empty",
                    throwable = it,
                    params = params
                )
                throw it
            }
        }
    }

    private fun getUserId(postApiKey: String) = abs(postApiKey.hashCode()).toString()
}
