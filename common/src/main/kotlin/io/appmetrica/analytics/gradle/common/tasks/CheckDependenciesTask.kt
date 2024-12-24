package io.appmetrica.analytics.gradle.common.tasks

import io.appmetrica.analytics.gradle.common.APPMETRICA_ARTIFACT
import io.appmetrica.analytics.gradle.common.APPMETRICA_LEGACY_ARTIFACT
import io.appmetrica.analytics.gradle.common.Log
import io.appmetrica.analytics.gradle.common.USING_TWO_APPMETRICAS_MESSAGE
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Make task cache is in progress")
abstract class CheckDependenciesTask : DefaultTask() {

    @get:Input
    abstract val rootComponent: Property<ResolvedComponentResult>

    @get:Input
    abstract val allowTwoAppMetricas: Property<Boolean>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun check() {
        val dependencies = findDependency(rootComponent.get(), APPMETRICA_ARTIFACT)
        val legacyDependencies = findDependency(rootComponent.get(), APPMETRICA_LEGACY_ARTIFACT)

        if (dependencies.isNotEmpty() && legacyDependencies.isNotEmpty()) {
            Log.warn(USING_TWO_APPMETRICAS_MESSAGE)
            (dependencies + legacyDependencies).forEach {
                Log.warn("Found dependency ${it.joinToString(" -> ")}")
            }
            if (!allowTwoAppMetricas.get()) {
                throw GradleException(USING_TWO_APPMETRICAS_MESSAGE)
            }
        }
    }

    private fun findDependency(
        initial: ResolvedComponentResult,
        dependencyToFind: String
    ): List<List<String>> {
        val viewedDependencies = mutableSetOf<String>()
        val result = mutableListOf<List<String>>()
        val path = mutableListOf<ResolvedComponentResult>()

        fun dps(current: ResolvedComponentResult) {
            val depName = current.id.displayName
            Log.debug("Searching in $depName")
            viewedDependencies.add(depName)
            path.add(current)
            if (dependencyToFind in depName) {
                val foundPath = path.map { it.id.displayName }
                Log.debug("Found at $foundPath")
                result.add(foundPath)
            } else {
                Log.info("Going to children")
                current.dependencies
                    .filterIsInstance<ResolvedDependencyResult>()
                    .filter { it.selected.id.displayName !in viewedDependencies }
                    .forEach { dps(it.selected) }
            }
            path.removeAt(path.size - 1)
        }
        dps(initial)
        return result
    }
}
