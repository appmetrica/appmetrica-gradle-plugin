package io.appmetrica.analytics.gradle.common.extension

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Main extension for AppMetrica Gradle plugin.
 *
 * Supports hierarchical configuration with priority:
 * global > variant > buildType > first flavor > default
 *
 * For `enable` properties uses special logic:
 * explicit false (in any source) > explicit true (in any source) > default
 *
 * Example usage:
 * ```kotlin
 * // Global settings
 * appmetrica {
 *     enableAnalytics.set(true)
 *     postApiKey.set("default-key")
 *
 *     ndk {
 *         enable.set(true)
 *     }
 * }
 *
 * // Per-buildType settings (via android extension)
 * android.buildTypes {
 *     named("release") {
 *         appmetrica {
 *             enable.set(true)
 *             postApiKey.set("release-key")
 *         }
 *     }
 *     named("debug") {
 *         appmetrica {
 *             enable.set(false)
 *             offline.set(true)
 *         }
 *     }
 * }
 *
 * // Per-flavor settings
 * android.productFlavors {
 *     named("prod") {
 *         appmetrica {
 *             postApiKey.set("prod-key")
 *         }
 *     }
 * }
 * ```
 */
abstract class AppMetricaExtension @Inject constructor(
    private val name: String,
    objects: ObjectFactory
) : Named {

    override fun getName(): String = name

    /**
     * Whether the plugin is enabled.
     * Default: true for "release" buildType, false otherwise
     */
    abstract val enable: Property<Boolean>

    /**
     * API key for uploading mapping/symbols to AppMetrica.
     * Required when enable is true and offline is false.
     */
    abstract val postApiKey: Property<String>

    /**
     * Offline mode - skip uploading to server.
     * Default: false
     */
    abstract val offline: Property<Boolean>

    /**
     * Custom mapping file path.
     * If not set, the default mapping file from the build will be used.
     */
    abstract val mappingFile: RegularFileProperty

    /**
     * Allow two versions of AppMetrica SDK in the project.
     * Default: false
     */
    abstract val allowTwoAppMetricas: Property<Boolean>

    /**
     * Enable plugin analytics collection.
     * Default: true
     */
    abstract val enableAnalytics: Property<Boolean>

    val ndk: AppMetricaNdkExtension = objects.newInstance(AppMetricaNdkExtension::class.java)

    fun ndk(action: Action<in AppMetricaNdkExtension>) {
        action.execute(ndk)
    }

    val variants: NamedDomainObjectContainer<AppMetricaExtension> =
        objects.domainObjectContainer(AppMetricaExtension::class.java)

    fun variants(action: Action<in NamedDomainObjectContainer<AppMetricaExtension>>) {
        action.execute(variants)
    }
}
