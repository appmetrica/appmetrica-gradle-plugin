package io.appmetrica.analytics.gradle.common

import org.gradle.api.logging.Logger

object Log {

    private var logger: Logger? = null

    fun setLogger(logger: Logger) {
        Log.logger = logger
    }

    fun error(data: String, throwable: Throwable? = null) {
        logger?.error(data, throwable)
    }

    fun warn(data: String, throwable: Throwable? = null) {
        logger?.warn(data, throwable)
    }

    fun info(data: String) {
        logger?.info(data)
    }

    fun debug(data: String) {
        logger?.debug(data)
    }
}
