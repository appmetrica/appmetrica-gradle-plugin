package io.appmetrica.analytics.gradle.plugin

import io.appmetrica.analytics.gradle.agp7.Agp7AndroidApplicationHelper
import io.appmetrica.analytics.gradle.agp8.Agp8AndroidApplicationHelper
import io.appmetrica.analytics.gradle.common.AGPVersion
import io.appmetrica.analytics.gradle.common.Log
import io.appmetrica.analytics.gradle.common.VersionNumber
import io.appmetrica.analytics.gradle.common.api.AndroidApplicationHelper
import org.gradle.api.GradleException
import org.gradle.api.Project

class AndroidApplicationHelperCreator(
    private val project: Project
) {

    companion object {

        private val MIN_AGP_VERSION = VersionNumber.parse("7.2.0")

        // AGP 8.1.0 introduced new Variant API that requires separate handling
        private val AGP8_VERSION = VersionNumber.parse("8.1.0")
    }

    /**
     * Creates an appropriate AndroidApplicationHelper based on the AGP version.
     *
     * Version mapping:
     * - AGP 7.2.0 to 8.0.x: Agp7AndroidApplicationHelper (uses legacy ApplicationVariant API)
     * - AGP 8.1.0+: Agp8AndroidApplicationHelper (uses new Variant API)
     */
    fun create(): AndroidApplicationHelper {
        val agpVersion = AGPVersion.current(project)
        Log.info("Found AGP version : $agpVersion")
        return when {
            agpVersion < MIN_AGP_VERSION -> throw GradleException(
                "Android Gradle Plugin version $agpVersion is not supported. " +
                    "Minimum required version is $MIN_AGP_VERSION. " +
                    "Please upgrade your AGP version."
            )
            agpVersion < AGP8_VERSION -> Agp7AndroidApplicationHelper(project)
            else -> Agp8AndroidApplicationHelper(project)
        }
    }
}
