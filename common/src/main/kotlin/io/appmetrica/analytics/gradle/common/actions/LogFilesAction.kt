package io.appmetrica.analytics.gradle.common.actions

import io.appmetrica.analytics.gradle.common.tasks.GenerateSymbolsTask
import io.appmetrica.analytics.gradle.common.utils.Log
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Zip

class LogFilesAction(
    private val message: String
) : Action<Task> {

    override fun execute(task: Task) {
        val files = when (task) {
            is Zip -> task.source.files
            is GenerateSymbolsTask -> task.files.asFileTree.files
            else -> return
        }
        Log.info(message)
        files.forEach { file ->
            Log.info("\t- ${file.canonicalPath}")
        }
    }
}
