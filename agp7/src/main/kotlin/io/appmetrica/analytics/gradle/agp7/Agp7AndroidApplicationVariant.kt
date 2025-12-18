package io.appmetrica.analytics.gradle.agp7

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.tasks.ExternalNativeBuildTask
import io.appmetrica.analytics.gradle.common.Log
import io.appmetrica.analytics.gradle.common.api.AndroidApplicationVariant
import io.appmetrica.analytics.gradle.common.uppercaseFirstChar
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File

class Agp7AndroidApplicationVariant(
    private val project: Project,
    private val original: ApplicationVariant
) : AndroidApplicationVariant {

    override val name: String
        get() = original.name

    override val versionName: Provider<String?>
        get() = project.provider { original.versionName }

    override val versionCode: Provider<Int?>
        get() = project.provider { original.versionCode }

    override val splitVersionCodes: Provider<Set<Int>>
        get() = project.provider { original.outputs.map { it.versionCode }.toSet() }

    override val mappingFile: Provider<File?>
        get() = original.mappingFileProvider.map { it.singleFile }

    override val applicationId: String
        get() = original.applicationId

    override fun subscribeOnAssembleTask(task: TaskProvider<out DefaultTask>) {
        original.assembleProvider.configure { it.finalizedBy(task) }
        project.tasks.named("bundle${original.name.uppercaseFirstChar()}").configure { it.finalizedBy(task) }
    }

    override fun addGenerateResourceTask(
        task: TaskProvider<out DefaultTask>,
        property: (DefaultTask) -> DirectoryProperty
    ) {
        original.registerResGeneratingTask(task.get(), property(task.get()).get().asFile)
    }

    override val defaultSoFiles: Provider<Set<File>>
        get() = project.provider {
            try {
                val externalNativeBuildProviders = original::class.java.getMethod("getExternalNativeBuildProviders")
                (externalNativeBuildProviders.invoke(original) as Collection<TaskProvider<ExternalNativeBuildTask>>)
                    .flatMapTo(mutableSetOf()) { it.get().objFolder.asFileTree.files }
            } catch (e: Throwable) {
                Log.info("Method getExternalNativeBuildProviders not found. Using getExternalNativeBuildTasks")
                val externalNativeBuildTasks = original::class.java.getMethod("getExternalNativeBuildTasks")
                (externalNativeBuildTasks.invoke(original) as Collection<ExternalNativeBuildTask>)
                    .flatMapTo(mutableSetOf()) { it.objFolder.asFileTree.files }
            }
        }
}
