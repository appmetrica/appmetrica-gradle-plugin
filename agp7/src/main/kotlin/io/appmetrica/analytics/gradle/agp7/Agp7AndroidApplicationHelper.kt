package io.appmetrica.analytics.gradle.agp7

import com.android.build.gradle.AppExtension
import io.appmetrica.analytics.gradle.agp7.extension.AppMetricaExtension
import io.appmetrica.analytics.gradle.agp7.extension.AppMetricaPluginConfigCreator
import io.appmetrica.analytics.gradle.common.APPMETRICA_PLUGIN
import io.appmetrica.analytics.gradle.common.api.AndroidApplicationHelper
import io.appmetrica.analytics.gradle.common.api.AndroidApplicationVariant
import io.appmetrica.analytics.gradle.common.config.AppMetricaPluginConfig
import org.gradle.api.Project

class Agp7AndroidApplicationHelper(
    private val project: Project
) : AndroidApplicationHelper {

    override fun hasAndroidPlugin() = project.plugins.hasPlugin("android")

    override fun configureEachVariant(block: (AndroidApplicationVariant, AppMetricaPluginConfig) -> Unit) {
        val extension = project.extensions.create(APPMETRICA_PLUGIN, AppMetricaExtension::class.java)
        val configCreator = AppMetricaPluginConfigCreator(extension)

        (project.extensions.getByName("android") as AppExtension).applicationVariants
            .all {
                val androidApplicationVariant = Agp7AndroidApplicationVariant(project, it)
                val config = configCreator.create(it)
                block(androidApplicationVariant, config)
            }
    }
}
