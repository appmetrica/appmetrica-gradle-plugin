package io.appmetrica.analytics.gradle.agp8

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.AppPlugin
import io.appmetrica.analytics.gradle.agp8.extension.AppMetricaExtension
import io.appmetrica.analytics.gradle.agp8.extension.AppMetricaPluginConfigCreator
import io.appmetrica.analytics.gradle.common.APPMETRICA_PLUGIN
import io.appmetrica.analytics.gradle.common.api.AndroidApplicationHelper
import io.appmetrica.analytics.gradle.common.api.AndroidApplicationVariant
import io.appmetrica.analytics.gradle.common.config.AppMetricaPluginConfig
import org.gradle.api.Project

class Agp8AndroidApplicationHelper(
    private val project: Project
) : AndroidApplicationHelper {

    override fun hasAndroidPlugin() = project.plugins.hasPlugin("android")

    override fun configureEachVariant(block: (AndroidApplicationVariant, AppMetricaPluginConfig) -> Unit) {
        val extension = project.extensions.create(APPMETRICA_PLUGIN, AppMetricaExtension::class.java)
        val configCreator = AppMetricaPluginConfigCreator(extension)

        project.plugins.withType(AppPlugin::class.java) {
            project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java).onVariants { variant ->
                val androidApplicationVariant = Agp8AndroidApplicationVariant(project, variant)
                val config = configCreator.create(variant)
                block(androidApplicationVariant, config)
            }
        }
    }
}
