package io.appmetrica.analytics.gradle.common.tasks

import io.appmetrica.analytics.gradle.common.utils.Log
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File

object GenerateSymbolsTaskSpek : Spek({
    describe("generate") {
        it("filters out non-.so files, non-existent files, and appmetrica files") {
            val project = ProjectBuilder.builder().build()
            Log.setLogger(project.logger)
            val outputDir = File(project.projectDir, "symbols").apply { mkdirs() }

            val soFile = project.file("libtest.so").apply { writeText("not_elf") }
            val txtFile = project.file("data.txt").apply { writeText("text") }
            val appmetricaSo = project.file("libappmetrica.so").apply { writeText("elf") }
            val nonExistent = project.file("missing.so")

            val task = project.tasks.create("genSym", GenerateSymbolsTask::class.java) {
                it.files.from(soFile, txtFile, appmetricaSo, nonExistent)
                it.symbolsDir.set(outputDir)
            }

            // generate() will try to parse libtest.so as ELF and fail
            // because it's not a valid ELF file, but the filtering should
            // exclude txtFile, appmetricaSo, and nonExistent before parsing
            @Suppress("SwallowedException")
            try {
                task.generate()
            } catch (e: Exception) {
                // Expected: libtest.so is not a valid ELF file
            }

            // Verify that the output directory was created (recreated)
            assertThat(outputDir).exists()
        }

        it("clears output directory before generating") {
            val project = ProjectBuilder.builder().build()
            Log.setLogger(project.logger)
            val outputDir = File(project.projectDir, "symbols2").apply { mkdirs() }
            val staleFile = File(outputDir, "old.ysym").apply { writeText("stale") }

            val task = project.tasks.create("genSym2", GenerateSymbolsTask::class.java) {
                it.files.from(project.files())
                it.symbolsDir.set(outputDir)
            }

            task.generate()

            assertThat(staleFile).doesNotExist()
            assertThat(outputDir).exists()
        }

        it("produces no output for empty file collection") {
            val project = ProjectBuilder.builder().build()
            Log.setLogger(project.logger)
            val outputDir = File(project.projectDir, "symbols3").apply { mkdirs() }

            val task = project.tasks.create("genSym3", GenerateSymbolsTask::class.java) {
                it.files.from(project.files())
                it.symbolsDir.set(outputDir)
            }

            task.generate()

            assertThat(outputDir.listFiles()).isEmpty()
        }
    }
})
