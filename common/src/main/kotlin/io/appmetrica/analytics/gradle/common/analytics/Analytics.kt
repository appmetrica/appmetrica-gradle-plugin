package io.appmetrica.analytics.gradle.common.analytics

interface Analytics {

    fun reportEvent(
        name: String,
        value: String? = null,
        params: Map<String, Any> = emptyMap()
    )

    fun reportError(
        message: String,
        throwable: Throwable? = null,
        params: Map<String, Any> = emptyMap()
    )
}
