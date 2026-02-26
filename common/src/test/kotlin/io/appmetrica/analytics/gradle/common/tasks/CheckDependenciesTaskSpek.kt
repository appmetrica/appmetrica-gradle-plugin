package io.appmetrica.analytics.gradle.common.tasks

import io.appmetrica.analytics.gradle.common.APPMETRICA_ARTIFACT
import io.appmetrica.analytics.gradle.common.APPMETRICA_LEGACY_ARTIFACT
import io.appmetrica.analytics.gradle.common.USING_TWO_APPMETRICAS_MESSAGE
import io.appmetrica.analytics.gradle.common.utils.Log
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.GradleException
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.testfixtures.ProjectBuilder
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object CheckDependenciesTaskSpek : Spek({

    beforeGroup {
        Log.setLogger(ProjectBuilder.builder().build().logger)
    }

    describe("check") {
        it("passes when no appmetrica dependencies found") {
            val project = ProjectBuilder.builder().build()
            val task = project.tasks.create("checkDeps", CheckDependenciesTask::class.java) {
                it.rootComponent.set(createComponent("root", emptyList()))
                it.allowTwoAppMetricas.set(false)
                it.outputFile.set(project.layout.buildDirectory.file("report.txt"))
            }

            assertThatCode { task.check() }.doesNotThrowAnyException()
        }

        it("passes when only new appmetrica artifact found") {
            val project = ProjectBuilder.builder().build()
            val appMetricaDep = createComponent("$APPMETRICA_ARTIFACT:5.0.0", emptyList())
            val root = createComponent("root", listOf(appMetricaDep))
            val task = project.tasks.create("checkDeps", CheckDependenciesTask::class.java) {
                it.rootComponent.set(root)
                it.allowTwoAppMetricas.set(false)
                it.outputFile.set(project.layout.buildDirectory.file("report.txt"))
            }

            assertThatCode { task.check() }.doesNotThrowAnyException()
        }

        it("passes when only legacy appmetrica artifact found") {
            val project = ProjectBuilder.builder().build()
            val legacyDep = createComponent("${APPMETRICA_LEGACY_ARTIFACT}5.0.0", emptyList())
            val root = createComponent("root", listOf(legacyDep))
            val task = project.tasks.create("checkDeps", CheckDependenciesTask::class.java) {
                it.rootComponent.set(root)
                it.allowTwoAppMetricas.set(false)
                it.outputFile.set(project.layout.buildDirectory.file("report.txt"))
            }

            assertThatCode { task.check() }.doesNotThrowAnyException()
        }

        it("throws when both artifacts found and allowTwoAppMetricas is false") {
            val project = ProjectBuilder.builder().build()
            val newDep = createComponent("$APPMETRICA_ARTIFACT:5.0.0", emptyList())
            val legacyDep = createComponent("${APPMETRICA_LEGACY_ARTIFACT}3.0.0", emptyList())
            val root = createComponent("root", listOf(newDep, legacyDep))
            val task = project.tasks.create("checkDeps", CheckDependenciesTask::class.java) {
                it.rootComponent.set(root)
                it.allowTwoAppMetricas.set(false)
                it.outputFile.set(project.layout.buildDirectory.file("report.txt"))
            }

            assertThatThrownBy { task.check() }
                .isInstanceOf(GradleException::class.java)
                .hasMessage(USING_TWO_APPMETRICAS_MESSAGE)
        }

        it("does not throw when both artifacts found but allowTwoAppMetricas is true") {
            val project = ProjectBuilder.builder().build()
            val newDep = createComponent("$APPMETRICA_ARTIFACT:5.0.0", emptyList())
            val legacyDep = createComponent("${APPMETRICA_LEGACY_ARTIFACT}3.0.0", emptyList())
            val root = createComponent("root", listOf(newDep, legacyDep))
            val task = project.tasks.create("checkDeps", CheckDependenciesTask::class.java) {
                it.rootComponent.set(root)
                it.allowTwoAppMetricas.set(true)
                it.outputFile.set(project.layout.buildDirectory.file("report.txt"))
            }

            assertThatCode { task.check() }.doesNotThrowAnyException()
        }

        it("finds nested dependencies in the graph") {
            val project = ProjectBuilder.builder().build()
            val legacyDep = createComponent("${APPMETRICA_LEGACY_ARTIFACT}3.0.0", emptyList())
            val middleDep = createComponent("some:library:1.0", listOf(legacyDep))
            val newDep = createComponent("$APPMETRICA_ARTIFACT:5.0.0", emptyList())
            val root = createComponent("root", listOf(middleDep, newDep))
            val task = project.tasks.create("checkDeps", CheckDependenciesTask::class.java) {
                it.rootComponent.set(root)
                it.allowTwoAppMetricas.set(false)
                it.outputFile.set(project.layout.buildDirectory.file("report.txt"))
            }

            assertThatThrownBy { task.check() }
                .isInstanceOf(GradleException::class.java)
                .hasMessage(USING_TWO_APPMETRICAS_MESSAGE)
        }
    }
})

private fun createComponent(
    displayName: String,
    children: List<ResolvedComponentResult>
): ResolvedComponentResult {
    val id = mock<org.gradle.api.artifacts.component.ComponentIdentifier> {
        on { this.displayName } doReturn displayName
    }
    val dependencies = children.map { child ->
        mock<ResolvedDependencyResult> {
            on { selected } doReturn child
        }
    }
    return mock {
        on { this.id } doReturn id
        on { this.dependencies } doReturn dependencies.toSet()
    }
}
