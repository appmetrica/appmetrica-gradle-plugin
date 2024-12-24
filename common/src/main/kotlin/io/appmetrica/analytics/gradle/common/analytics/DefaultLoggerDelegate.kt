package io.appmetrica.analytics.gradle.common.analytics

import com.yandex.browser.rtm.LoggerDelegate
import io.appmetrica.analytics.gradle.common.Log

class DefaultLoggerDelegate : LoggerDelegate {

    override fun d(tag: String, msg: String) = Log.debug("[$tag] $msg")

    override fun e(tag: String, msg: String, throwable: Throwable?) = Log.error("[$tag] $msg", throwable)

    override fun w(tag: String, msg: String, throwable: Throwable?) = Log.warn("[$tag] $msg", throwable)
}
