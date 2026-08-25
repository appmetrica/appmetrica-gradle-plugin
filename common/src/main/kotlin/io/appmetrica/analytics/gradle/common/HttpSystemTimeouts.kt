package io.appmetrica.analytics.gradle.common

import java.time.Duration

internal object HttpSystemTimeouts {
    const val CONNECTION_TIMEOUT_PROPERTY = "http.connection.timeout"

    fun connectTimeout(): Duration? =
        System.getProperty(CONNECTION_TIMEOUT_PROPERTY)
            ?.toLongOrNull()
            ?.takeIf { it >= 0L }
            ?.let { Duration.ofMillis(it) }
}
