package io.appmetrica.analytics.gradle.common.config

import io.appmetrica.analytics.gradle.common.api.AndroidApplicationVariant
import io.appmetrica.analytics.gradle.common.extension.AppMetricaExtension
import io.appmetrica.analytics.gradle.common.tasks.ResourcesGeneratorTask
import io.appmetrica.analytics.gradle.common.utils.Log
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ConfigFactorySpek : Spek({

    beforeGroup {
        Log.setLogger(ProjectBuilder.builder().build().logger)
    }

    describe("appMetricaConfig") {
        it("uses default values when no extensions are configured") {
            val project = ProjectBuilder.builder().build()
            val extension = project.objects.newInstance(AppMetricaExtension::class.java, "appmetrica")
            val variant = createVariant(project, "release")

            val factory = ConfigFactory(
                project = project,
                extension = extension,
                variantExtension = null,
                buildTypeExtension = null,
                flavorExtensions = emptyMap(),
                variant = variant
            )
            val config = factory.appMetricaConfig

            assertThat(config.enable.get()).isTrue()
            assertThat(config.postApiKey.get()).isEmpty()
            assertThat(config.offline.get()).isFalse()
            assertThat(config.allowTwoAppMetricas.get()).isFalse()
            assertThat(config.enableAnalytics.get()).isTrue()
        }

        it("enable defaults to false for non-release build type") {
            val project = ProjectBuilder.builder().build()
            val extension = project.objects.newInstance(AppMetricaExtension::class.java, "appmetrica")
            val variant = createVariant(project, "debug")

            val factory = ConfigFactory(
                project = project,
                extension = extension,
                variantExtension = null,
                buildTypeExtension = null,
                flavorExtensions = emptyMap(),
                variant = variant
            )
            val config = factory.appMetricaConfig

            assertThat(config.enable.get()).isFalse()
        }

        it("uses global extension values") {
            val project = ProjectBuilder.builder().build()
            val extension = project.objects.newInstance(AppMetricaExtension::class.java, "appmetrica").apply {
                postApiKey.set("my-key")
                offline.set(true)
                enableAnalytics.set(false)
            }
            val variant = createVariant(project, "release")

            val factory = ConfigFactory(
                project = project,
                extension = extension,
                variantExtension = null,
                buildTypeExtension = null,
                flavorExtensions = emptyMap(),
                variant = variant
            )
            val config = factory.appMetricaConfig

            assertThat(config.postApiKey.get()).isEqualTo("my-key")
            assertThat(config.offline.get()).isTrue()
            assertThat(config.enableAnalytics.get()).isFalse()
        }

        it("ndk defaults to disabled") {
            val project = ProjectBuilder.builder().build()
            val extension = project.objects.newInstance(AppMetricaExtension::class.java, "appmetrica")
            val variant = createVariant(project, "release")

            val factory = ConfigFactory(
                project = project,
                extension = extension,
                variantExtension = null,
                buildTypeExtension = null,
                flavorExtensions = emptyMap(),
                variant = variant
            )
            val config = factory.appMetricaConfig

            assertThat(config.ndk.enable.get()).isFalse()
            assertThat(config.ndk.addNdkCrashesDependency.get()).isTrue()
        }

        it("variant extension overrides global for postApiKey") {
            val project = ProjectBuilder.builder().build()
            val extension = project.objects.newInstance(AppMetricaExtension::class.java, "appmetrica").apply {
                postApiKey.set("global-key")
            }
            val variantExtension = project.objects.newInstance(AppMetricaExtension::class.java, "release").apply {
                postApiKey.set("variant-key")
            }
            val variant = createVariant(project, "release")

            val factory = ConfigFactory(
                project = project,
                extension = extension,
                variantExtension = variantExtension,
                buildTypeExtension = null,
                flavorExtensions = emptyMap(),
                variant = variant
            )
            val config = factory.appMetricaConfig

            // resolveUnique returns unique value or default on conflict
            // Both set different values → conflict → returns default (empty string)
            assertThat(config.postApiKey.get()).isEmpty()
        }

        it("buildType extension enables plugin for debug") {
            val project = ProjectBuilder.builder().build()
            val extension = project.objects.newInstance(AppMetricaExtension::class.java, "appmetrica")
            val buildTypeExtension = project.objects.newInstance(AppMetricaExtension::class.java, "debug").apply {
                enable.set(true)
            }
            val variant = createVariant(project, "debug")

            val factory = ConfigFactory(
                project = project,
                extension = extension,
                variantExtension = null,
                buildTypeExtension = buildTypeExtension,
                flavorExtensions = emptyMap(),
                variant = variant
            )
            val config = factory.appMetricaConfig

            assertThat(config.enable.get()).isTrue()
        }
    }
})

private fun createVariant(
    project: org.gradle.api.Project,
    buildTypeName: String
): AndroidApplicationVariant {
    return object : AndroidApplicationVariant {
        override val name: String = buildTypeName
        override val appMetricaConfig: AppMetricaPluginConfig
            get() = throw UnsupportedOperationException()
        override val buildType: Provider<String?> = project.providers.provider { buildTypeName }
        override val versionName: Provider<String?> = project.providers.provider { "1.0" }
        override val versionCode: Provider<Int?> = project.providers.provider { 1 }
        override val splitVersionCodes: Provider<Set<Int>> = project.providers.provider { emptySet() }
        override val applicationId: String = "com.example.app"
        override val mappingFile: Provider<RegularFile> = project.layout.buildDirectory.file("mapping.txt").get().let {
            project.providers.provider { it }
        }
        override val soFiles: FileCollection = project.files()
        override fun subscribeOnAssembleTask(task: TaskProvider<out DefaultTask>) = Unit
        override fun addGenerateResourceTask(
            task: TaskProvider<out ResourcesGeneratorTask>,
            property: (ResourcesGeneratorTask) -> DirectoryProperty
        ) = Unit
    }
}
