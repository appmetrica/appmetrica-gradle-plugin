package io.appmetrica.analytics.gradle.common.analytics

import com.yandex.browser.rtm.RTMLib
import com.yandex.browser.rtm.RTMUploadScheduler
import io.appmetrica.analytics.gradle.common.utils.Log

class DefaultUploadScheduler : RTMUploadScheduler {

    private val tag = "[DefaultUploadScheduler]"

    override fun schedule(eventPayload: String) {
        val result = RTMLib.uploadEventAndWaitResult(eventPayload)
        Log.debug("$tag Sent $eventPayload with result code ${result.httpResultCode}")
    }
}
