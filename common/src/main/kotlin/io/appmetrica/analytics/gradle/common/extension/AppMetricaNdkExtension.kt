package io.appmetrica.analytics.gradle.common.extension

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property

/**
 * Global NDK extension for AppMetrica plugin.
 *
 * Per-buildType and per-flavor NDK settings are configured via:
 * ```kotlin
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
