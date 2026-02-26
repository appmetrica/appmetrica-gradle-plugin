package io.appmetrica.analytics.gradle.common.actions

import io.appmetrica.analytics.gradle.common.NO_MAPPING_FILE_MESSAGE
import io.appmetrica.analytics.gradle.common.utils.Log
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ValidateMappingFileActionSpek : Spek({
    describe("execute") {
        it("throws GradleException when mapping file does not exist") {
            val project = ProjectBuilder.builder().build()
            Log.setLogger(project.logger)
            val mappingFile = project.layout.projectDirectory.file("nonexistent.txt")
            val provider = project.providers.provider { mappingFile }
            val action = ValidateMappingFileAction(provider)
            val task = project.tasks.create("testTask")

            assertThatThrownBy { action.execute(task) }
                .isInstanceOf(GradleException::class.java)
                .hasMessage(NO_MAPPING_FILE_MESSAGE)
        }

        it("does not throw when mapping file exists") {
            val project = ProjectBuilder.builder().build()
            Log.setLogger(project.logger)
            val file = project.file("mapping.txt").apply { writeText("test") }
            val mappingFile = project.layout.projectDirectory.file(file.name)
            val provider = project.providers.provider { mappingFile }
            val action = ValidateMappingFileAction(provider)
            val task = project.tasks.create("testTask")

            assertThatCode { action.execute(task) }.doesNotThrowAnyException()
        }

        it("throws GradleException when provider has no value") {
            val project = ProjectBuilder.builder().build()
            Log.setLogger(project.logger)
            val provider = project.objects.fileProperty()
            val action = ValidateMappingFileAction(provider)
            val task = project.tasks.create("testTask")

            assertThatThrownBy { action.execute(task) }
                .isInstanceOf(GradleException::class.java)
                .hasMessage(NO_MAPPING_FILE_MESSAGE)
        }
    }
})
