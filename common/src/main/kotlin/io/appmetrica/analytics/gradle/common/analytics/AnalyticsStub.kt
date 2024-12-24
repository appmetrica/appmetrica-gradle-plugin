package io.appmetrica.analytics.gradle.common.analytics

class AnalyticsStub : Analytics {

    override fun reportEvent(name: String, value: String?, params: Map<String, Any>) {
        // do nothing
    }

    override fun reportError(message: String, throwable: Throwable?, params: Map<String, Any>) {
        // do nothing
    }
}
