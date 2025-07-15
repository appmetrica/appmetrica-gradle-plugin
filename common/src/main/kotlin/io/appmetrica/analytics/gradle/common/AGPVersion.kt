package io.appmetrica.analytics.gradle.common

import org.gradle.api.Project

object AGPVersion {

    fun current(project: Project): VersionNumber {
        return getFromVersionString() ?: getFromClasspath(project)
    }

    private fun getFromVersionString(): VersionNumber? {
        return try {
            VersionNumber.parse(com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION)
        } catch (throwable: Throwable) {
            Log.warn("Failed to parse AGP version from string.", throwable)
            null
        }
    }

    private fun getFromClasspath(project: Project): VersionNumber {
        val depAGP = project.rootProject.buildscript.configurations.getByName("classpath").dependencies.find {
            (it.group == "com.android.tools.build" && it.name == "gradle") ||
                (it.group == "com.android.application" && it.name == "com.android.application.gradle.plugin")
        } ?: throw IllegalStateException("Failed to find AGP dependency.")

        return VersionNumber.parse(depAGP.version ?: "")
    }
}
