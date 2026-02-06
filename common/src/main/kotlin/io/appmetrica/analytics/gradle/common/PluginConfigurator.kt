package io.appmetrica.analytics.gradle.common

import io.appmetrica.analytics.gradle.common.api.AndroidApplicationVariant
import io.appmetrica.analytics.gradle.common.config.AppMetricaPluginConfig
import io.appmetrica.analytics.gradle.common.tasks.CheckDependenciesTask
import io.appmetrica.analytics.gradle.common.tasks.GenerateSymbolsTask
import io.appmetrica.analytics.gradle.common.tasks.ResourcesGeneratorTask
import io.appmetrica.analytics.gradle.common.tasks.UploadTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.util.GradleVersion

@Suppress("TooManyFunctions")
class PluginConfigurator {

    fun configure(
        project: Project,
        variant: AndroidApplicationVariant,
        config: AppMetricaPluginConfig
    ) {
        if (config.enable()) {
            configureCommon(project, variant, config)
        }
        if (config.ndk.enable()) {
            configureNdk(project, variant, config)
        }
    }

    private fun configureCommon(
        project: Project,
        variant: AndroidApplicationVariant,
        config: AppMetricaPluginConfig
    ) {
        Log.info(
            """
                Configuring ${variant.name}:
                    offline = ${config.offline()} 
                    ndk.enable = ${config.ndk.enable()}
            """.trimIndent()
        )

        val uploadTask = getOrCreateUploadMappingTask(project, variant, config)

        project.afterEvaluate {
            variant.subscribeOnAssembleTask(uploadTask)
        }
    }

    private fun configureNdk(
        project: Project,
        variant: AndroidApplicationVariant,
        config: AppMetricaPluginConfig
    ) {
        Log.info("Configuring ndk ${variant.name}")
        if (config.ndk.addNdkCrashesDependency()) {
            project.dependencies.add(variant.name + "Implementation", APPMETRICA_NDK_PLUGIN)
        }

        getOrCreateUploadNdkSymbolsTask(project, variant, config)
    }

    private fun getOrCreateCheckDependenciesTask(
        project: Project,
        variant: AndroidApplicationVariant,
        config: AppMetricaPluginConfig
    ): TaskProvider<CheckDependenciesTask> {
        val taskName = "check${variant.name.uppercaseFirstChar()}AppMetricaDependencies"
        project.tasks.findNamed(taskName, CheckDependenciesTask::class.java)?.let {
            return it
        }
        return project.tasks.register(taskName, CheckDependenciesTask::class.java) { task ->
            task.rootComponent.set(
                project.configurations
                    .getByName("${variant.name}RuntimeClasspath")
                    .incoming
                    .resolutionResult
                    .rootComponent
            )
            task.outputFile.set(project.appMetricaBuildDir(variant).map { it.file("report.txt") })
            task.allowTwoAppMetricas.set(config.allowTwoAppMetricas())
        }
    }

    private fun getOrCreateUploadMappingTask(
        project: Project,
        variant: AndroidApplicationVariant,
        config: AppMetricaPluginConfig
    ): TaskProvider<UploadTask> {
        val taskName = "upload${variant.name.uppercaseFirstChar()}AppMetricaMapping"
        project.tasks.findNamed(taskName, UploadTask::class.java)?.let {
            return it
        }
        val zipFilesTask = getOrCreateZipFilesTask(project, variant, config)
        val checkDependenciesTask = getOrCreateCheckDependenciesTask(project, variant, config)
        return project.tasks.register(taskName, UploadTask::class.java) { task ->
            task.zipFile.set(zipFilesTask.flatMap { it.archiveFile })

            task.uploadUrl.set(MAPPING_UPLOAD_URL)
            task.postApiKey.set(config.postApiKey())
            task.offline.set(config.offline())
            task.enableAnalytics.set(config.enableAnalytics)
            task.paramsForAnalytics.set(
                mapOf("task_type" to "upload_mapping") + getParamsForAnalytics(project, variant, config)
            )

            task.dependsOn(checkDependenciesTask)
        }
    }

    @Suppress("UseCheckOrError")
    private fun getOrCreateZipFilesTask(
        project: Project,
        variant: AndroidApplicationVariant,
        config: AppMetricaPluginConfig
    ): TaskProvider<Zip> {
        val taskName = "zip${variant.name.uppercaseFirstChar()}AppMetricaFiles"
        project.tasks.findNamed(taskName, Zip::class.java)?.let {
            return it
        }
        val resCreationTask = getOrCreateResourceTask(project, variant, config)
        val mappingFileProvider = if (config.mappingFile() == null) {
            Log.info("Getting mapping file for variant ${variant.name} from variant")
            variant.mappingFile
        } else {
            Log.info("Getting mapping file for variant ${variant.name} from config")
            project.provider {
                config.mappingFile() ?: throw IllegalStateException(
                    "Minify is not enabled for variant ${variant.name}."
                )
            }
        }
        return project.tasks.register(taskName, Zip::class.java) { task ->
            task.from(resCreationTask.flatMap { it.buildInfoFile })
            task.from(mappingFileProvider)
            task.archiveFileName.set("mapping.zip")
            task.destinationDirectory.set(project.appMetricaBuildDir(variant).map { it.dir("result") })

            task.doFirst {
                task.source.files.forEach { file ->
                    Log.info("Zipping file ${file.canonicalPath}")
                }
            }
        }
    }

    private fun getOrCreateUploadNdkSymbolsTask(
        project: Project,
        variant: AndroidApplicationVariant,
        config: AppMetricaPluginConfig
    ): TaskProvider<UploadTask> {
        val taskName = "upload${variant.name.uppercaseFirstChar()}AppMetricaNdkSymbols"
        project.tasks.findNamed(taskName, UploadTask::class.java)?.let {
            return it
        }
        val zipFilesTask = getOrCreateZipNdkFilesTask(project, variant, config)
        return project.tasks.register(taskName, UploadTask::class.java) { task ->
            task.zipFile.set(zipFilesTask.flatMap { it.archiveFile })

            task.uploadUrl.set(SYMBOLS_UPLOAD_URL)
            task.postApiKey.set(config.postApiKey())
            task.offline.set(config.offline())
            task.enableAnalytics.set(config.enableAnalytics)
            task.paramsForAnalytics.set(
                mapOf("task_type" to "upload_ndk") + getParamsForAnalytics(project, variant, config)
            )
        }
    }

    private fun getOrCreateZipNdkFilesTask(
        project: Project,
        variant: AndroidApplicationVariant,
        config: AppMetricaPluginConfig
    ): TaskProvider<Zip> {
        val taskName = "zip${variant.name.uppercaseFirstChar()}AppMetricaNdkFiles"
        project.tasks.findNamed(taskName, Zip::class.java)?.let {
            return it
        }
        val resCreationTask = getOrCreateResourceTask(project, variant, config)
        val generateSymbolsTask = getOrCreateGenerateSymbolsTask(project, variant, config)
        return project.tasks.register(taskName, Zip::class.java) { task ->
            task.from(resCreationTask.flatMap { it.buildInfoFile })
            task.from(generateSymbolsTask.flatMap { it.symbolsDir })
            task.archiveFileName.set("symbols.zip")
            task.destinationDirectory.set(project.appMetricaBuildDir(variant).map { it.dir("result") })

            task.doFirst {
                task.source.files.forEach { file ->
                    Log.info("Zipping ndk file ${file.canonicalPath}")
                }
            }
        }
    }

    private fun getOrCreateGenerateSymbolsTask(
        project: Project,
        variant: AndroidApplicationVariant,
        config: AppMetricaPluginConfig
    ): TaskProvider<GenerateSymbolsTask> {
        val taskName = "generate${variant.name.uppercaseFirstChar()}AppMetricaNdkSymbols"
        project.tasks.findNamed(taskName, GenerateSymbolsTask::class.java)?.let {
            return it
        }
        val soFilesProvider = variant.defaultSoFiles.map {
            if (config.ndk.soFiles().isEmpty()) {
                it + config.ndk.additionalSoFiles().toSet()
            } else {
                config.ndk.soFiles().toSet() + config.ndk.additionalSoFiles().toSet()
            }
        }
        return project.tasks.register(taskName, GenerateSymbolsTask::class.java) { task ->
            task.files.from(soFilesProvider)
            task.symbolsDir.set(project.appMetricaBuildDir(variant).map { it.dir("symbols") })
        }
    }

    private fun getOrCreateResourceTask(
        project: Project,
        variant: AndroidApplicationVariant,
        config: AppMetricaPluginConfig
    ): TaskProvider<ResourcesGeneratorTask> {
        val taskName = "create${variant.name.uppercaseFirstChar()}AppMetricaRes"
        project.tasks.findNamed(taskName, ResourcesGeneratorTask::class.java)?.let {
            return it
        }
        val taskProvider = project.tasks.register(taskName, ResourcesGeneratorTask::class.java) { task ->
            task.versionName.set(variant.versionName)
            task.versionCode.set(variant.versionCode)
            task.mappingType.set(
                if (((project.properties["android.enableR8"] ?: "false") as String).toBoolean()) {
                    MappingType.R8
                } else {
                    MappingType.PROGUARD
                }
            )
            task.splitVersionCodes.set(variant.splitVersionCodes)
            task.offline.set(config.offline())
            task.ndkEnable.set(config.ndk.enable())

            task.buildInfoFile.set(project.appMetricaBuildDir(variant).map { it.file("info.txt") })
            task.resDirectory.set(project.appMetricaBuildDir(variant).map { it.dir("res") })
        }
        variant.addGenerateResourceTask(taskProvider) { (it as ResourcesGeneratorTask).resDirectory }

        return taskProvider
    }

    private fun getParamsForAnalytics(
        project: Project,
        variant: AndroidApplicationVariant,
        config: AppMetricaPluginConfig
    ): Map<String, Any> {
        return mapOf(
            "offline" to config.offline(),
            "ndk.enable" to config.ndk.enable(),
            "versionName" to (variant.versionName.orNull ?: ""),
            "versionCode" to (variant.versionCode.orNull ?: 0),
            "applicationId" to variant.applicationId,
            "agp.version" to AGPVersion.current(project).toString(),
            "gradle.version" to GradleVersion.current().version
        )
    }

    private fun Project.appMetricaBuildDir(
        variant: AndroidApplicationVariant
    ): Provider<Directory> {
        return layout.buildDirectory.dir("appmetrica/${variant.name}")
    }
}
