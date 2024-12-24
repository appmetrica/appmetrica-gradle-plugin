package io.appmetrica.analytics.gradle.agp8

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationVariant
import io.appmetrica.analytics.gradle.common.Log
import io.appmetrica.analytics.gradle.common.api.AndroidApplicationVariant
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File

class Agp8AndroidApplicationVariant(
    private val project: Project,
    private val original: ApplicationVariant
) : AndroidApplicationVariant {

    override val name: String
        get() = original.name

    override val versionName: Provider<String?>
        get() = original.outputs.first().versionName

    override val versionCode: Provider<Int?>
        get() = original.outputs.first().versionCode

    override val splitVersionCodes: Provider<Set<Int>>
        get() = project.provider { original.outputs.mapNotNull { it.versionCode.get() }.toSet() }

    override val mappingFile: Provider<File?>
        get() = try {
            original.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE).map { it.asFile }
        } catch (e: Throwable) {
            Log.error("Failed to get mapping file", e)
            Log.error("Make sure that obfuscation is enabled or disable the loading of mappings in the plugin setting")
            Log.error("If this does not help, then refer to the documentation")
            project.provider { null }
        }

    override val applicationId: String
        get() = original.applicationId.get()

    override fun subscribeOnAssembleTask(task: TaskProvider<out DefaultTask>) {
        project.tasks.named("assemble${original.name.capitalize()}").configure { it.finalizedBy(task) }
        project.tasks.named("bundle${original.name.capitalize()}").configure { it.finalizedBy(task) }
    }

    override fun addGenerateResourceTask(
        task: TaskProvider<out DefaultTask>,
        property: (DefaultTask) -> DirectoryProperty
    ) {
        original.sources.res?.addGeneratedSourceDirectory(task, property)
    }

    override val defaultSoFiles: Provider<Set<File>>
        get() = original.artifacts.get(SingleArtifact.MERGED_NATIVE_LIBS).map { it.asFileTree.files }
}
