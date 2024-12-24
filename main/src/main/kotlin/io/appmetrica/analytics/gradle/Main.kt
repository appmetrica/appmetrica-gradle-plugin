package io.appmetrica.analytics.gradle

import io.appmetrica.analytics.gradle.common.Log
import io.appmetrica.analytics.gradle.common.ndk.ElfYSymFactory
import io.appmetrica.analytics.gradle.common.ndk.YSymSerializer
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.io.File

fun main() {
    Log.setLogger(PrintingLogger())
    listOf("obj").forEach { objDirPath ->
        File(objDirPath).walk().filter { it.extension == "so" }.forEach { soFile ->
            println("Parse ${soFile.absolutePath}")
            File(soFile.parent, "${soFile.nameWithoutExtension}.ysym").outputStream().use { ysymFile ->
                ysymFile.write(YSymSerializer.toString(ElfYSymFactory().createCSymFromFile(soFile)).toByteArray())
            }
        }
    }
}

class PrintingLogger : Logger by Logging.getLogger(Project::class.java) {
    override fun debug(value: String?) {
        println(value)
    }

    override fun error(value: String?) {
        println(value)
    }

    override fun warn(value: String?) {
        println(value)
    }

    override fun info(value: String?) {
        println(value)
    }
}
