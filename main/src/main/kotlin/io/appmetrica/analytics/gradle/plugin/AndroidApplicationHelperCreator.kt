package io.appmetrica.analytics.gradle.plugin

import io.appmetrica.analytics.gradle.agp4.Agp4AndroidApplicationHelper
import io.appmetrica.analytics.gradle.agp7.Agp7AndroidApplicationHelper
import io.appmetrica.analytics.gradle.agp8.Agp8AndroidApplicationHelper
import io.appmetrica.analytics.gradle.common.AGPVersion
import io.appmetrica.analytics.gradle.common.Log
import io.appmetrica.analytics.gradle.common.VersionNumber
import io.appmetrica.analytics.gradle.common.api.AndroidApplicationHelper
import org.gradle.api.Project

class AndroidApplicationHelperCreator(
    private val project: Project
) {

    private val agp7Version = VersionNumber.parse("7.2.0")
    private val agp8Version = VersionNumber.parse("8.1.0")

    fun create(): AndroidApplicationHelper {
        val agpVersion = AGPVersion.current(project)
        Log.info("Found AGP version : $agpVersion")
        return when {
            agpVersion < agp7Version -> Agp4AndroidApplicationHelper(project)
            agpVersion < agp8Version -> Agp7AndroidApplicationHelper(project)
            else -> Agp8AndroidApplicationHelper(project)
        }
    }
}
