package io.appmetrica.analytics.gradle.plugin

import io.appmetrica.analytics.gradle.common.Log
import io.appmetrica.analytics.gradle.common.PluginConfigurator
import org.gradle.api.Plugin
import org.gradle.api.Project

class AppMetricaPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        Log.setLogger(project.logger)
        val helper = AndroidApplicationHelperCreator(project).create()
        if (helper.hasAndroidPlugin()) {
            helper.configureEachVariant { variant, config ->
                PluginConfigurator().configure(project, variant, config)
            }
        } else {
            Log.error("ERROR: plugin requires Android Gradle Plugin (AGP)")
        }
    }
}
