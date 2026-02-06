package io.appmetrica.analytics.gradle.common.config

import java.io.File

@Suppress("LongParameterList")
class AppMetricaPluginConfig(
    val enable: () -> Boolean,
    val postApiKey: () -> String,
    val offline: () -> Boolean,
    val mappingFile: () -> File?,
    val enableAnalytics: Boolean,
    val allowTwoAppMetricas: () -> Boolean,
    val ndk: AppMetricaPluginNdkConfig
)
