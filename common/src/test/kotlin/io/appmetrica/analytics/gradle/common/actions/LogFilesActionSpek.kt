package io.appmetrica.analytics.gradle.common.actions

import io.appmetrica.analytics.gradle.common.utils.Log
import org.assertj.core.api.Assertions.assertThatCode
import org.gradle.api.logging.Logger
import org.gradle.testfixtures.ProjectBuilder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object LogFilesActionSpek : Spek({
    describe("execute") {
        it("logs files for Zip task") {
            val project = ProjectBuilder.builder().build()
            val logger = mock<Logger>()
            Log.setLogger(logger)

            val file1 = project.file("a.txt").apply { writeText("a") }
            val file2 = project.file("b.txt").apply { writeText("b") }
            val zipTask = project.tasks.create(
                "testZip",
                org.gradle.api.tasks.bundling.Zip::class.java
            ) {
                it.from(file1, file2)
            }
            val action = LogFilesAction("Zipping files:")

            assertThatCode { action.execute(zipTask) }.doesNotThrowAnyException()
            verify(logger).info("Zipping files:")
        }

        it("logs files for GenerateSymbolsTask") {
            val project = ProjectBuilder.builder().build()
            val logger = mock<Logger>()
            Log.setLogger(logger)

            val file = project.file("lib.so").apply { writeText("elf") }
            val task = project.tasks.create(
                "genSymbols",
                io.appmetrica.analytics.gradle.common.tasks.GenerateSymbolsTask::class.java
            ) {
                it.files.from(file)
            }
            val action = LogFilesAction("Processing so files:")

            assertThatCode { action.execute(task) }.doesNotThrowAnyException()
            verify(logger).info("Processing so files:")
        }

        it("does nothing for unrecognized task type") {
            val project = ProjectBuilder.builder().build()
            val logger = mock<Logger>()
            Log.setLogger(logger)

            val task = project.tasks.create("plainTask")
            val action = LogFilesAction("test:")

            action.execute(task)
            verify(logger, never()).info("test:")
        }
    }
})
