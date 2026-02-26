package io.appmetrica.analytics.gradle.common.utils

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.testfixtures.ProjectBuilder
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object AGPVersionSpek : Spek({
    describe("current") {
        it("throws IllegalStateException when AGP is not on classpath") {
            val project = ProjectBuilder.builder().build()
            Log.setLogger(project.logger)

            // ProjectBuilder creates a project without AGP in buildscript,
            // and com.android.Version is not on the common module test classpath,
            // so both resolution strategies should fail
            assertThatThrownBy { AGPVersion.current(project) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Failed to find AGP dependency.")
        }

        it("falls back to classpath when Version class is available but project has AGP dependency") {
            val project = ProjectBuilder.builder().build()
            Log.setLogger(project.logger)

            // Add a fake AGP dependency to the buildscript classpath
            project.rootProject.buildscript.dependencies.add(
                "classpath",
                project.rootProject.dependencies.create("com.android.tools.build:gradle:8.2.2")
            )

            val version = AGPVersion.current(project)
            assertThat(version).isEqualTo(VersionNumber.parse("8.2.2"))
        }
    }
})
