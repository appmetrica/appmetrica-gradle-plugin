package io.appmetrica.analytics.gradle.common.config

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

@Suppress("LongParameterList")
class AppMetricaPluginConfig(
    val enable: Provider<Boolean>,
    val postApiKey: Provider<String>,
    val offline: Provider<Boolean>,
    val mappingFile: Provider<RegularFile>,
    val allowTwoAppMetricas: Provider<Boolean>,
    val enableAnalytics: Provider<Boolean>,
    val ndk: AppMetricaPluginNdkConfig
)
