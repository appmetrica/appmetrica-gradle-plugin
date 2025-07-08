package io.appmetrica.analytics.gradle.common

import org.gradle.api.Project

object AGPVersion {

    fun current(project: Project): VersionNumber {
        val depAGP = project.rootProject.buildscript.configurations.getByName("classpath").dependencies.find {
            (it.group == "com.android.tools.build" && it.name == "gradle") ||
                (it.group == "com.android.application" && it.name == "com.android.application.gradle.plugin")
        } ?: throw IllegalStateException("Failed to find AGP dependency.")

        return VersionNumber.parse(depAGP.version ?: "")
    }
}
