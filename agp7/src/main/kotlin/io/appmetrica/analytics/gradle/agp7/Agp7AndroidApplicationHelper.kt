package io.appmetrica.analytics.gradle.agp7

import com.android.build.gradle.AppExtension
import io.appmetrica.analytics.gradle.common.APPMETRICA_PLUGIN
import io.appmetrica.analytics.gradle.common.api.AndroidApplicationHelper
import io.appmetrica.analytics.gradle.common.api.AndroidApplicationVariant
import io.appmetrica.analytics.gradle.common.extension.AppMetricaExtension
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

class Agp7AndroidApplicationHelper(
    private val project: Project
) : AndroidApplicationHelper {

    override fun hasAndroidPlugin() = project.plugins.hasPlugin("android")

    override fun configureEachVariant(block: (AndroidApplicationVariant) -> Unit) {
        // Register main appmetrica extension
        project.extensions.create(
            APPMETRICA_PLUGIN,
            AppMetricaExtension::class.java,
            APPMETRICA_PLUGIN
        )

        val androidExtension = project.extensions.getByName("android") as AppExtension

        // Register appmetrica extension on each BuildType
        androidExtension.buildTypes.all { buildType ->
            (buildType as ExtensionAware).extensions.create(
                APPMETRICA_PLUGIN,
                AppMetricaExtension::class.java,
                APPMETRICA_PLUGIN
            )
        }

        // Register appmetrica extension on each ProductFlavor
        androidExtension.productFlavors.all { flavor ->
            (flavor as ExtensionAware).extensions.create(
                APPMETRICA_PLUGIN,
                AppMetricaExtension::class.java,
                APPMETRICA_PLUGIN
            )
        }

        // Configure variants
        androidExtension.applicationVariants.all { variant ->
            val androidApplicationVariant = Agp7AndroidApplicationVariant(project, variant)
            block(androidApplicationVariant)
        }
    }
}
