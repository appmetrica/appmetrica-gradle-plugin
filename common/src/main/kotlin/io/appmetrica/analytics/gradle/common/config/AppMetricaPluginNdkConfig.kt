package io.appmetrica.analytics.gradle.common.config

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider

class AppMetricaPluginNdkConfig(
    val enable: Provider<Boolean>,
    val soFiles: FileCollection,
    val additionalSoFiles: FileCollection,
    val addNdkCrashesDependency: Provider<Boolean>
)
