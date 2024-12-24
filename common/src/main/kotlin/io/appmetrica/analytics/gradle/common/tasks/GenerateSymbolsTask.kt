package io.appmetrica.analytics.gradle.common.tasks

import io.appmetrica.analytics.gradle.common.Log
import io.appmetrica.analytics.gradle.common.ndk.ElfYSymFactory
import io.appmetrica.analytics.gradle.common.ndk.YSymSerializer
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.io.File

@UntrackedTask(because = "Make task cache is in progress")
abstract class GenerateSymbolsTask : DefaultTask() {

    @get:InputFiles
    abstract val files: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val symbolsDir: DirectoryProperty

    @TaskAction
    fun generate() {
        symbolsDir.get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }
        files.files.filter {
            it.exists() && it.isFile && it.name.endsWith(".so") && !it.name.contains("appmetrica")
        }.forEach { generate(it) }
    }

    private fun generate(file: File) {
        Log.info("Taking symbols from ${file.absolutePath}")
        val symbols = ElfYSymFactory().createCSymFromFile(file)
        Log.info("\tUUID = ${symbols.identifier}")
        val symFile = symbolsDir.get().asFile.resolve("${file.nameWithoutExtension}_${symbols.identifier}.ysym")
        symFile.writeText(YSymSerializer.toString(symbols))
    }
}
