package io.appmetrica.analytics.gradle.agp8

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.AppPlugin
import io.appmetrica.analytics.gradle.common.APPMETRICA_PLUGIN
import io.appmetrica.analytics.gradle.common.api.AndroidApplicationHelper
import io.appmetrica.analytics.gradle.common.api.AndroidApplicationVariant
import io.appmetrica.analytics.gradle.common.extension.AppMetricaExtension
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

class Agp8AndroidApplicationHelper(
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

        project.plugins.withType(AppPlugin::class.java) {
            val androidExtension = project.extensions.getByType(ApplicationExtension::class.java)

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
            project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java).onVariants { variant ->
                val androidApplicationVariant = Agp8AndroidApplicationVariant(project, variant)
                block(androidApplicationVariant)
            }
        }
    }
}
