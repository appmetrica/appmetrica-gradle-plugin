package io.appmetrica.analytics.gradle.common.api

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File

interface AndroidApplicationVariant {

    val name: String

    val versionName: Provider<String?>

    val versionCode: Provider<Int?>

    val splitVersionCodes: Provider<Set<Int>>

    val mappingFile: Provider<File?>

    val applicationId: String

    fun subscribeOnAssembleTask(task: TaskProvider<out DefaultTask>)

    fun addGenerateResourceTask(
        task: TaskProvider<out DefaultTask>,
        property: (DefaultTask) -> DirectoryProperty
    )

    val defaultSoFiles: Provider<Set<File>>
}
