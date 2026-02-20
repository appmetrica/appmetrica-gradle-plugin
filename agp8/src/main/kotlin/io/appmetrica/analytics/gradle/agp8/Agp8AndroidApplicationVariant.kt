package io.appmetrica.analytics.gradle.agp8

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationVariant
import io.appmetrica.analytics.gradle.common.api.AndroidApplicationVariant
import io.appmetrica.analytics.gradle.common.config.AppMetricaPluginConfig
import io.appmetrica.analytics.gradle.common.config.ConfigFactory
import io.appmetrica.analytics.gradle.common.extension.AppMetricaExtension
import io.appmetrica.analytics.gradle.common.tasks.ResourcesGeneratorTask
import io.appmetrica.analytics.gradle.common.utils.uppercaseFirstChar
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

class Agp8AndroidApplicationVariant(
    private val project: Project,
    private val original: ApplicationVariant
) : AndroidApplicationVariant {

    private val androidExtension: ApplicationExtension = project.extensions.getByType(ApplicationExtension::class.java)

    private val extension: AppMetricaExtension = project.extensions.getByType(AppMetricaExtension::class.java)

    private val buildTypeExtension: AppMetricaExtension? = original.buildType?.let { buildTypeName ->
        androidExtension
            .buildTypes
            .findByName(buildTypeName)
            ?.let { buildType ->
                (buildType as? ExtensionAware)
                    ?.extensions
                    ?.findByType(AppMetricaExtension::class.java)
            }
    }

    private val flavorExtensions: Map<String, AppMetricaExtension> =
        original.productFlavors.mapNotNull { (_, flavorName) ->
            androidExtension
                .productFlavors
                .findByName(flavorName)
                ?.let { flavor ->
                    (flavor as? ExtensionAware)
                        ?.extensions
                        ?.findByType(AppMetricaExtension::class.java)
                }
                ?.let {
                    flavorName to it
                }
        }.toMap()

    private val variantExtension: AppMetricaExtension? = extension.variants.findByName(original.name)

    override val appMetricaConfig: AppMetricaPluginConfig by lazy {
        ConfigFactory(
            project,
            extension,
            variantExtension,
            buildTypeExtension,
            flavorExtensions,
            this
        ).appMetricaConfig
    }

    override val name: String
        get() = original.name

    override val buildType: Provider<String?>
        get() = project.provider { original.buildType }

    override val versionName: Provider<String?>
        get() = original.outputs.first().versionName

    override val versionCode: Provider<Int?>
        get() = original.outputs.first().versionCode

    override val splitVersionCodes: Provider<Set<Int>>
        get() = project.provider { original.outputs.mapNotNull { it.versionCode.get() }.toSet() }

    override val applicationId: String
        get() = original.applicationId.get()

    override val mappingFile: Provider<RegularFile>
        get() = original.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE)

    override val soFiles: FileCollection
        get() = project.files(
            original.artifacts.get(SingleArtifact.MERGED_NATIVE_LIBS)
        )

    override fun subscribeOnAssembleTask(task: TaskProvider<out DefaultTask>) {
        project.tasks.named("assemble${original.name.uppercaseFirstChar()}").configure { it.finalizedBy(task) }
        project.tasks.named("bundle${original.name.uppercaseFirstChar()}").configure { it.finalizedBy(task) }
    }

    override fun addGenerateResourceTask(
        task: TaskProvider<out ResourcesGeneratorTask>,
        property: (ResourcesGeneratorTask) -> DirectoryProperty
    ) {
        original.sources.res?.addGeneratedSourceDirectory(task, property)
    }
}
