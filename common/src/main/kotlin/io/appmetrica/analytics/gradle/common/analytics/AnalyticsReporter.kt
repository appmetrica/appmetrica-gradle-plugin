package io.appmetrica.analytics.gradle.common.analytics

import com.google.gson.Gson
import com.yandex.browser.rtm.RTMLib
import io.appmetrica.analytics.gradle.common.RTM_PROJECT_NAME
import io.appmetrica.analytics.gradle.common.RTM_VERSION

class AnalyticsReporter(
    private val userId: String
) : Analytics {

    init {
        RTMLib.initializeAppHostStatics(RTMLib.appHostStaticsBuilder().loggerDelegate(DefaultLoggerDelegate()))
    }

    private val rtm: RTMLib = RTMLib.builder(
        projectName = RTM_PROJECT_NAME,
        version = RTM_VERSION,
        uploadScheduler = DefaultUploadScheduler()
    ).userIdProvider { userId }.build()

    override fun reportEvent(
        name: String,
        value: String?,
        params: Map<String, Any>
    ) {
        rtm.newEventBuilder(name, value)
            .setAdditional(Gson().toJson(params))
            .send()
    }

    override fun reportError(
        message: String,
        throwable: Throwable?,
        params: Map<String, Any>
    ) {
        rtm.newErrorBuilder(message)
            .setStackTrace(throwable)
            .setAdditional(Gson().toJson(params))
            .send()
    }
}
