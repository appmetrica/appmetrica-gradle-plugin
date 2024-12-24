package io.appmetrica.analytics.gradle.common.api

import io.appmetrica.analytics.gradle.common.config.AppMetricaPluginConfig

interface AndroidApplicationHelper {

    fun hasAndroidPlugin(): Boolean

    fun configureEachVariant(block: (AndroidApplicationVariant, AppMetricaPluginConfig) -> Unit)
}
