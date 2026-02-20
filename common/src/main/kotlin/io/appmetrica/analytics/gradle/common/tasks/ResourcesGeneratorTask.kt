package io.appmetrica.analytics.gradle.common.tasks

import com.google.gson.Gson
import io.appmetrica.analytics.gradle.common.BUILD_ID_KEY
import io.appmetrica.analytics.gradle.common.BuildInfo
import io.appmetrica.analytics.gradle.common.IS_OFFLINE_KEY
import io.appmetrica.analytics.gradle.common.LEGACY_BUILD_ID_KEY
import io.appmetrica.analytics.gradle.common.LEGACY_IS_OFFLINE_KEY
import io.appmetrica.analytics.gradle.common.LEGACY_NDK_ENABLE_KEY
import io.appmetrica.analytics.gradle.common.LEGACY_RES_VALUE_PREFIX
import io.appmetrica.analytics.gradle.common.MappingType
import io.appmetrica.analytics.gradle.common.NDK_ENABLE_KEY
import io.appmetrica.analytics.gradle.common.RES_VALUE_PREFIX
import io.appmetrica.analytics.gradle.common.utils.Log
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.util.UUID

@UntrackedTask(because = "Make task cache is in progress")
abstract class ResourcesGeneratorTask : DefaultTask() {

    @get:Input
    abstract val versionName: Property<String?>

    @get:Input
    abstract val versionCode: Property<Int?>

    @get:Input
    abstract val mappingType: Property<MappingType>

    @get:Input
    abstract val splitVersionCodes: SetProperty<Int>

    @get:Input
    abstract val offline: Property<Boolean>

    @get:Input
    abstract val ndkEnable: Property<Boolean>

    @get:OutputDirectory
    abstract val resDirectory: DirectoryProperty

    @get:OutputFile
    abstract val buildInfoFile: RegularFileProperty

    @TaskAction
    fun taskAction() {
        val buildId = UUID.randomUUID().toString()
        Log.info(
            "Generating resources for versionName = `$versionName` and versionCode = `$versionCode` " +
                "with build id : $buildId"
        )

        createKeepResFile()
        createBuildIdResFile(buildId)
        createInfoFile(buildId)
    }

    private fun createKeepResFile() {
        val resDir = resDirectory.asFile.get()
            .resolve("raw")
            .also { it.mkdirs() }
        val keepList = listOf(
            LEGACY_RES_VALUE_PREFIX,
            RES_VALUE_PREFIX
        ).flatMap { prefix ->
            listOf(
                "@string",
                "@bool"
            ).map { type ->
                "$type/$prefix*"
            }
        }.joinToString(",")
        resDir.resolve("keep_appmetrica_resources.xml")
            .writeText(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <resources
                        xmlns:tools="http://schemas.android.com/tools"
                        tools:keep="$keepList"
                    />
                """.trimIndent()
            )
    }

    private fun createBuildIdResFile(buildId: String) {
        val resDir = resDirectory.asFile.get()
            .resolve("values")
            .also { it.mkdirs() }
        resDir.resolve("appmetrica_resources.xml")
            .writeText(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <resources xmlns:tools="http://schemas.android.com/tools">
                        <string name="$LEGACY_BUILD_ID_KEY">$buildId</string>
                        <string name="$BUILD_ID_KEY">$buildId</string>
                        <bool name="$LEGACY_IS_OFFLINE_KEY">${offline.get()}</bool>
                        <bool name="$IS_OFFLINE_KEY">${offline.get()}</bool>
                        <bool name="$LEGACY_NDK_ENABLE_KEY">${ndkEnable.get()}</bool>
                        <bool name="$NDK_ENABLE_KEY">${ndkEnable.get()}</bool>
                    </resources>
                """.trimIndent()
            )
    }

    private fun createInfoFile(buildId: String) {
        val buildInfo = BuildInfo(
            buildId = buildId,
            versionName = versionName.orNull ?: "",
            versionCode = versionCode.orNull ?: 0,
            mappingType = mappingType.get(),
            splitVersionCodes = splitVersionCodes.getOrElse(emptySet())
        )
        buildInfoFile.get().asFile.writeText(Gson().toJson(buildInfo))
    }
}
