package io.appmetrica.analytics.gradle.common.api

interface AndroidApplicationHelper {

    fun hasAndroidPlugin(): Boolean

    fun configureEachVariant(block: (AndroidApplicationVariant) -> Unit)
}
