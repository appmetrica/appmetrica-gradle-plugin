package io.appmetrica.analytics.gradle.common

import io.appmetrica.analytics.gradle.common.actions.LogFilesAction
import io.appmetrica.analytics.gradle.common.actions.ValidateMappingFileAction
import io.appmetrica.analytics.gradle.common.api.AndroidApplicationVariant
import io.appmetrica.analytics.gradle.common.config.AppMetricaPluginConfig
import io.appmetrica.analytics.gradle.common.tasks.CheckDependenciesTask
import io.appmetrica.analytics.gradle.common.tasks.GenerateSymbolsTask
import io.appmetrica.analytics.gradle.common.tasks.ResourcesGeneratorTask
import io.appmetrica.analytics.gradle.common.tasks.UploadTask
import io.appmetrica.analytics.gradle.common.utils.AGPVersion
import io.appmetrica.analytics.gradle.common.utils.Log
import io.appmetrica.analytics.gradle.common.utils.findNamed
import io.appmetrica.analytics.gradle.common.utils.uppercaseFirstChar
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
        variant: AndroidApplicationVariant
    ) {
        val uploadMappingTask = getOrCreateUploadMappingTask(project, variant)
        getOrCreateUploadNdkSymbolsTask(project, variant)

        project.afterEvaluate {
            val config = variant.appMetricaConfig
            validateConfig(config, variant.name)

            if (config.enable.get()) {
                variant.subscribeOnAssembleTask(uploadMappingTask)
            }

            if (config.ndk.enable.get() && config.ndk.addNdkCrashesDependency.get()) {
                project.dependencies.add(variant.name + "Implementation", APPMETRICA_NDK_PLUGIN)
            }
        }
    }

    private fun getOrCreateCheckDependenciesTask(
        project: Project,
        variant: AndroidApplicationVariant
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
            task.allowTwoAppMetricas.set(variant.appMetricaConfig.allowTwoAppMetricas)
        }
    }

    private fun getOrCreateUploadMappingTask(
        project: Project,
        variant: AndroidApplicationVariant
    ): TaskProvider<UploadTask> {
        val taskName = "upload${variant.name.uppercaseFirstChar()}AppMetricaMapping"
        project.tasks.findNamed(taskName, UploadTask::class.java)?.let {
            return it
        }

        val zipFilesTask = getOrCreateZipFilesTask(project, variant)
        val checkDependenciesTask = getOrCreateCheckDependenciesTask(project, variant)

        return project.tasks.register(taskName, UploadTask::class.java) { task ->
            task.taskEnabled.set(variant.appMetricaConfig.enable)

            task.zipFile.set(zipFilesTask.flatMap { it.archiveFile })
            task.uploadUrl.set(MAPPING_UPLOAD_URL)
            task.postApiKey.set(variant.appMetricaConfig.postApiKey)
            task.offline.set(variant.appMetricaConfig.offline)
            task.enableAnalytics.set(variant.appMetricaConfig.enableAnalytics)
            task.paramsForAnalytics.set(
                getParamsForAnalytics(project, variant) + mapOf("task_type" to "upload_mapping")
            )

            task.dependsOn(checkDependenciesTask)
        }
    }

    @Suppress("UseCheckOrError")
    private fun getOrCreateZipFilesTask(
        project: Project,
        variant: AndroidApplicationVariant
    ): TaskProvider<Zip> {
        val taskName = "zip${variant.name.uppercaseFirstChar()}AppMetricaFiles"
        project.tasks.findNamed(taskName, Zip::class.java)?.let {
            return it
        }

        val resourceTask = getOrCreateResourceTask(project, variant)
        val mappingFileProvider = variant.appMetricaConfig.mappingFile

        return project.tasks.register(taskName, Zip::class.java) { task ->
            task.from(resourceTask.flatMap { it.buildInfoFile })
            task.from(mappingFileProvider)
            task.archiveFileName.set("mapping.zip")
            task.destinationDirectory.set(project.appMetricaBuildDir(variant).map { it.dir("result") })

            task.doFirst(ValidateMappingFileAction(mappingFileProvider))
            task.doFirst(LogFilesAction("Zipping files:"))
        }
    }

    private fun getOrCreateUploadNdkSymbolsTask(
        project: Project,
        variant: AndroidApplicationVariant
    ): TaskProvider<UploadTask> {
        val taskName = "upload${variant.name.uppercaseFirstChar()}AppMetricaNdkSymbols"
        project.tasks.findNamed(taskName, UploadTask::class.java)?.let {
            return it
        }

        val zipNdkFilesTask = getOrCreateZipNdkFilesTask(project, variant)

        return project.tasks.register(taskName, UploadTask::class.java) { task ->
            task.taskEnabled.set(variant.appMetricaConfig.ndk.enable)

            task.uploadUrl.set(SYMBOLS_UPLOAD_URL)
            task.postApiKey.set(variant.appMetricaConfig.postApiKey)
            task.offline.set(variant.appMetricaConfig.offline)
            task.zipFile.set(zipNdkFilesTask.flatMap { it.archiveFile })
            task.enableAnalytics.set(variant.appMetricaConfig.enableAnalytics)
            task.paramsForAnalytics.set(
                getParamsForAnalytics(project, variant) + mapOf("task_type" to "upload_ndk")
            )
        }
    }

    private fun getOrCreateZipNdkFilesTask(
        project: Project,
        variant: AndroidApplicationVariant
    ): TaskProvider<Zip> {
        val taskName = "zip${variant.name.uppercaseFirstChar()}AppMetricaNdkFiles"
        project.tasks.findNamed(taskName, Zip::class.java)?.let {
            return it
        }

        val createResourceTask = getOrCreateResourceTask(project, variant)
        val createGenerateSymbolsTask = getOrCreateGenerateSymbolsTask(project, variant)

        return project.tasks.register(taskName, Zip::class.java) { task ->
            task.from(createResourceTask.flatMap { it.buildInfoFile })
            task.from(createGenerateSymbolsTask.flatMap { it.symbolsDir })
            task.archiveFileName.set("symbols.zip")
            task.destinationDirectory.set(project.appMetricaBuildDir(variant).map { it.dir("result") })

            task.doFirst(LogFilesAction("Zipping NDK files:"))
        }
    }

    private fun getOrCreateGenerateSymbolsTask(
        project: Project,
        variant: AndroidApplicationVariant
    ): TaskProvider<GenerateSymbolsTask> {
        val taskName = "generate${variant.name.uppercaseFirstChar()}AppMetricaNdkSymbols"
        project.tasks.findNamed(taskName, GenerateSymbolsTask::class.java)?.let {
            return it
        }

        return project.tasks.register(taskName, GenerateSymbolsTask::class.java) { task ->
            task.files.from(variant.appMetricaConfig.ndk.soFiles)
            task.files.from(variant.appMetricaConfig.ndk.additionalSoFiles)
            task.symbolsDir.set(project.appMetricaBuildDir(variant).map { it.dir("symbols") })

            task.doFirst(LogFilesAction("Processing so files:"))
        }
    }

    private fun getOrCreateResourceTask(
        project: Project,
        variant: AndroidApplicationVariant
    ): TaskProvider<ResourcesGeneratorTask> {
        val taskName = "create${variant.name.uppercaseFirstChar()}AppMetricaRes"
        project.tasks.findNamed(taskName, ResourcesGeneratorTask::class.java)?.let {
            return it
        }

        return project.tasks.register(taskName, ResourcesGeneratorTask::class.java) { task ->
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
            task.offline.set(variant.appMetricaConfig.offline)
            task.ndkEnable.set(variant.appMetricaConfig.ndk.enable)

            task.buildInfoFile.set(project.appMetricaBuildDir(variant).map { it.file("info.txt") })
            task.resDirectory.set(project.appMetricaBuildDir(variant).map { it.dir("res") })
        }.also { taskProvider ->
            variant.addGenerateResourceTask(taskProvider) { it.resDirectory }
        }
    }

    private fun getParamsForAnalytics(
        project: Project,
        variant: AndroidApplicationVariant
    ): Map<String, Any> {
        return mapOf(
            "offline" to variant.appMetricaConfig.offline.get(),
            "ndk.enable" to variant.appMetricaConfig.ndk.enable.get(),
            "versionName" to (variant.versionName.orNull ?: ""),
            "versionCode" to (variant.versionCode.orNull ?: 0),
            "applicationId" to variant.applicationId,
            "agp.version" to AGPVersion.current(project).toString(),
            "gradle.version" to GradleVersion.current().version
        )
    }

    private fun validateConfig(config: AppMetricaPluginConfig, variantName: String) {
        val isEnabled = config.enable.get()
        val isNdkEnabled = config.ndk.enable.get()
        val isOffline = config.offline.get()
        val postApiKey = config.postApiKey.get()

        val needPostApiKey = (isEnabled || isNdkEnabled) && !isOffline
        val hasPostApiKey = postApiKey.isNotEmpty()
        if (needPostApiKey && !hasPostApiKey) {
            Log.warn(
                "AppMetrica plugin is enabled for variant '$variantName' but postApiKey is not set. " +
                    "Upload will fail. Set postApiKey or enable offline mode."
            )
        }
    }

    private fun Project.appMetricaBuildDir(
        variant: AndroidApplicationVariant
    ): Provider<Directory> {
        return layout.buildDirectory.dir("appmetrica/${variant.name}")
    }
}
