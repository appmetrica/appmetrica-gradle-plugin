package io.appmetrica.analytics.gradle.common.api

import io.appmetrica.analytics.gradle.common.MappingType
import io.appmetrica.analytics.gradle.common.config.AppMetricaPluginConfig
import io.appmetrica.analytics.gradle.common.tasks.ResourcesGeneratorTask
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

interface AndroidApplicationVariant {

    val name: String

    /**
     * Resolved AppMetrica plugin configuration for this variant.
     */
    val appMetricaConfig: AppMetricaPluginConfig

    val buildType: Provider<String?>

    val versionName: Provider<String?>

    val versionCode: Provider<Int?>

    val splitVersionCodes: Provider<Set<Int>>

    val applicationId: String

    val mappingType: MappingType

    val mappingFile: Provider<RegularFile>

    val soFiles: FileCollection

    fun subscribeOnAssembleTask(task: TaskProvider<out DefaultTask>)

    fun addGenerateResourceTask(
        task: TaskProvider<out ResourcesGeneratorTask>,
        property: (ResourcesGeneratorTask) -> DirectoryProperty
    )
}
