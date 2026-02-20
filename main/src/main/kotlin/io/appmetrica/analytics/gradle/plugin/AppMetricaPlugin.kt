package io.appmetrica.analytics.gradle.plugin

import io.appmetrica.analytics.gradle.common.PLUGIN_VERSION
import io.appmetrica.analytics.gradle.common.PluginConfigurator
import io.appmetrica.analytics.gradle.common.utils.Log
import org.gradle.api.Plugin
import org.gradle.api.Project

class AppMetricaPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        Log.setLogger(project.logger)
        Log.info("Running AppMetrica Gradle Plugin $PLUGIN_VERSION")

        val helper = AndroidApplicationHelperCreator(project).create()
        if (helper.hasAndroidPlugin()) {
            helper.configureEachVariant { variant ->
                PluginConfigurator().configure(project, variant)
            }
        } else {
            Log.error("ERROR: plugin requires Android Gradle Plugin (AGP)")
        }
    }
}
