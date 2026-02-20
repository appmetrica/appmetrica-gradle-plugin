package io.appmetrica.analytics.gradle.agp7

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.tasks.ExternalNativeBuildTask
import io.appmetrica.analytics.gradle.common.api.AndroidApplicationVariant
import io.appmetrica.analytics.gradle.common.config.AppMetricaPluginConfig
import io.appmetrica.analytics.gradle.common.config.ConfigFactory
import io.appmetrica.analytics.gradle.common.extension.AppMetricaExtension
import io.appmetrica.analytics.gradle.common.tasks.ResourcesGeneratorTask
import io.appmetrica.analytics.gradle.common.utils.Log
import io.appmetrica.analytics.gradle.common.utils.uppercaseFirstChar
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

class Agp7AndroidApplicationVariant(
    private val project: Project,
    private val original: ApplicationVariant
) : AndroidApplicationVariant {

    private val androidExtension: ApplicationExtension = project.extensions.getByType(ApplicationExtension::class.java)

    private val extension: AppMetricaExtension = project.extensions.getByType(AppMetricaExtension::class.java)

    private val buildTypeExtension: AppMetricaExtension? = androidExtension
        .buildTypes
        .findByName(original.buildType.name)
        ?.let { buildType ->
            (buildType as? ExtensionAware)
                ?.extensions
                ?.findByType(AppMetricaExtension::class.java)
        }

    private val flavorExtensions: Map<String, AppMetricaExtension> =
        original.productFlavors.mapNotNull { productFlavor ->
            androidExtension
                .productFlavors
                .findByName(productFlavor.name)
                ?.let { flavor ->
                    (flavor as? ExtensionAware)
                        ?.extensions
                        ?.findByType(AppMetricaExtension::class.java)
                }
                ?.let {
                    productFlavor.name to it
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
        get() = project.provider { original.buildType.name }

    override val versionName: Provider<String?>
        get() = project.provider { original.versionName }

    override val versionCode: Provider<Int?>
        get() = project.provider { original.versionCode }

    override val splitVersionCodes: Provider<Set<Int>>
        get() = project.provider { original.outputs.map { it.versionCode }.toSet() }

    override val applicationId: String
        get() = original.applicationId

    override val mappingFile: Provider<RegularFile>
        get() = original.mappingFileProvider
            .flatMap { it.elements }
            .flatMap { elements ->
                project.objects.fileProperty().also { prop ->
                    elements.singleOrNull()?.let { prop.set(it.asFile) }
                }
            }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override val soFiles: FileCollection
        get() = project.files(
            project.provider {
                try {
                    val externalNativeBuildProviders =
                        original::class.java.getMethod("getExternalNativeBuildProviders")
                            .invoke(original) as Collection<TaskProvider<ExternalNativeBuildTask>>
                    externalNativeBuildProviders
                        .flatMapTo(mutableSetOf()) { it.get().objFolder.asFileTree.files }
                } catch (e: Throwable) {
                    Log.info(
                        "Method getExternalNativeBuildProviders not found. " +
                            "Using getExternalNativeBuildTasks"
                    )
                    val externalNativeBuildTasks =
                        original::class.java.getMethod("getExternalNativeBuildTasks")
                            .invoke(original) as Collection<ExternalNativeBuildTask>
                    externalNativeBuildTasks
                        .flatMapTo(mutableSetOf()) { it.objFolder.asFileTree.files }
                }
            }
        )

    override fun subscribeOnAssembleTask(task: TaskProvider<out DefaultTask>) {
        original.assembleProvider.configure { it.finalizedBy(task) }
        project.tasks.named("bundle${original.name.uppercaseFirstChar()}").configure { it.finalizedBy(task) }
    }

    override fun addGenerateResourceTask(
        task: TaskProvider<out ResourcesGeneratorTask>,
        property: (ResourcesGeneratorTask) -> DirectoryProperty
    ) {
        original.registerResGeneratingTask(task.get(), property(task.get()).get().asFile)
    }
}
