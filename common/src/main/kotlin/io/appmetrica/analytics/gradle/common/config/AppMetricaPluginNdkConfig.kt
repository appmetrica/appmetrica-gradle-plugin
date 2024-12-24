package io.appmetrica.analytics.gradle.common.config

import java.io.File

class AppMetricaPluginNdkConfig(
    val enable: () -> Boolean,
    val soFiles: () -> List<File>,
    val additionalSoFiles: () -> List<File>,
    val addNdkCrashesDependency: () -> Boolean
)
