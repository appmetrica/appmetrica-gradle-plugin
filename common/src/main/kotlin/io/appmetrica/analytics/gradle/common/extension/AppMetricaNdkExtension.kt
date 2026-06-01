package io.appmetrica.analytics.gradle.common.extension

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property

/**
 * Global NDK extension for AppMetrica plugin.
 *
 * Per-buildType and per-flavor NDK settings are configured via:
 * ```kotlin
 * // For Kotlin DSL, import the matching extension function for your AGP version:
 * //   import io.appmetrica.analytics.gradle.agp8.appmetrica  // for AGP 8.x
 * //   import io.appmetrica.analytics.gradle.agp7.appmetrica  // for AGP 7.x
 *
 * android.buildTypes {
 *     named("release") {
 *         appmetrica {
 *             ndk {
 *                 enable.set(true)
 *             }
 *         }
 *     }
 * }
 * ```
 */
abstract class AppMetricaNdkExtension {

    /**
     * Whether NDK symbols processing is enabled.
     * Default: false
     */
    abstract val enable: Property<Boolean>

    /**
     * Custom .so files to process (replaces default ones).
     * If empty, default .so files from the build will be used.
     */
    abstract val soFiles: ConfigurableFileCollection

    /**
     * Additional .so files to process (added to default ones).
     */
    abstract val additionalSoFiles: ConfigurableFileCollection

    /**
     * Whether to automatically add appmetrica-ndk-crashes dependency.
     * Default: true
     */
    abstract val addNdkCrashesDependency: Property<Boolean>
}
