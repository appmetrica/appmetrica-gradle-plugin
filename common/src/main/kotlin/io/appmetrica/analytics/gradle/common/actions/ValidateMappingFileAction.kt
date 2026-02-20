package io.appmetrica.analytics.gradle.common.actions

import io.appmetrica.analytics.gradle.common.NO_MAPPING_FILE_MESSAGE
import io.appmetrica.analytics.gradle.common.utils.Log
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

class ValidateMappingFileAction(
    private val mappingFileProvider: Provider<RegularFile>
) : Action<Task> {

    override fun execute(task: Task) {
        if (!mappingFileProvider.isPresent || !mappingFileProvider.get().asFile.exists()) {
            Log.error(NO_MAPPING_FILE_MESSAGE)
            throw GradleException(NO_MAPPING_FILE_MESSAGE)
        }
    }
}
