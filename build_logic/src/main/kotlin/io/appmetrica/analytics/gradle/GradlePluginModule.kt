package io.appmetrica.analytics.gradle

import io.appmetrica.gradle.common.plugins.KotlinLibraryPlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class GradlePluginModule : Plugin<Project> {

    override fun apply(project: Project) {
        project.apply<KotlinLibraryPlugin>()
        project.apply<JacocoPlugin>() // jacoco

        project.group = Constants.Library.group
        project.version = Constants.Library.versionName + (project.properties["versionPostfix"] ?: "")

        project.createEmbedConfiguration()
        project.configureElfFixturesRebuild()
        project.configureTests()
        project.configureJacoco()
        project.configureJavaVersion()
        project.configureKotlinVersion()

        val appMetricaGradlePluginLibs =
            project.extensions.getByType<VersionCatalogsExtension>().named("appMetricaGradlePluginLibs")

        project.dependencies {
            val implementation by project.configurations.getting
            val testImplementation by project.configurations.getting
            val testRuntimeOnly by project.configurations.getting

            implementation(localGroovy())
            implementation(gradleApi())

            implementation(appMetricaGradlePluginLibs.findLibrary("kotlin-stdlib").get())
            implementation(appMetricaGradlePluginLibs.findLibrary("gson").get())

            testImplementation(appMetricaGradlePluginLibs.findLibrary("junit").get())
            testImplementation(appMetricaGradlePluginLibs.findLibrary("assertj").get())
            testImplementation(appMetricaGradlePluginLibs.findLibrary("mockito-kotlin").get())
            testImplementation(appMetricaGradlePluginLibs.findLibrary("mockito-core").get())

            testImplementation(appMetricaGradlePluginLibs.findLibrary("spek-dsl").get())
            testRuntimeOnly(appMetricaGradlePluginLibs.findLibrary("spek-runner").get())
            testRuntimeOnly(appMetricaGradlePluginLibs.findLibrary("junit-platform-launcher").get())
            testRuntimeOnly(appMetricaGradlePluginLibs.findLibrary("kotlin-reflect").get())
            testImplementation(appMetricaGradlePluginLibs.findLibrary("mockserver").get())
        }
    }

    private fun Project.configureElfFixturesRebuild() {
        val source = layout.projectDirectory.file("elf-fixtures/fixture.c")
        val extraSource = layout.projectDirectory.file("elf-fixtures/fixture_extra.c")
        if (!source.asFile.isFile || !extraSource.asFile.isFile) return

        tasks.register("rebuildElfFixtures", RebuildElfFixtures::class.java) {
            group = "verification"
            description = "Rebuilds checked-in ELF test fixtures with Android NDK r27c"
            ndkPath.set(
                providers.gradleProperty("androidNdkPath")
                    .orElse(providers.environmentVariable("ANDROID_NDK_HOME"))
            )
            androidHome.set(
                providers.environmentVariable("ANDROID_HOME")
                    .orElse(providers.environmentVariable("ANDROID_SDK_ROOT"))
            )
            fixtureSource.set(source)
            extraFixtureSource.set(extraSource)
            outputDirectory.set(layout.projectDirectory.dir("src/test/resources/elf-fixtures"))
            outputs.upToDateWhen { false }
        }
    }

    private fun Project.createEmbedConfiguration() {
        val embed: Configuration by project.configurations.creating

        // Direct artifacts only. Resolving `embed` itself also pulls the runtime
        // classpath of embedded projects, including gradleApi(). Gradle 9.5+ shades
        // BouncyCastle multi-release classes (major version 69); packing those into
        // the plugin jar breaks InstrumentationAnalysisTransform on Gradle <= 8.13.
        val embeddedArtifacts = project.configurations.resolvable("embeddedArtifacts") {
            extendsFrom(embed)
            isTransitive = false
        }.get()

        project.configurations.named("compileOnly") {
            extendsFrom(embed)
        }

        project.tasks.named<Jar>("jar") {
            dependsOn(embeddedArtifacts)
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            from({
                embeddedArtifacts.map { artifact ->
                    if (artifact.isDirectory) {
                        artifact
                    } else {
                        // Signed deps (BouncyCastle, JGit, …) leave META-INF/*.SF|*.RSA in the
                        // fat jar; after merge digests no longer match and Groovy script
                        // compilation fails with SecurityException.
                        zipTree(artifact.canonicalFile).matching {
                            exclude(
                                "META-INF/*.SF",
                                "META-INF/*.DSA",
                                "META-INF/*.RSA",
                                "META-INF/*.EC",
                            )
                        }
                    }
                }
            })
        }
    }

    private fun Project.configureTests() {
        tasks.named<Test>("test") {
            useJUnitPlatform {
                includeEngines("spek2")
            }
            jvmArgs(
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED"
            )
        }
    }

    private fun Project.configureJacoco() {
        tasks.create("runUnitTestsWithCoverage") {
            dependsOn(tasks.named("test"))
            finalizedBy(tasks.named("jacocoTestReport"))
        }
    }

    private fun Project.configureJavaVersion() {
        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }

    private fun Project.configureKotlinVersion() {
        tasks.withType<KotlinCompile> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_11)
                freeCompilerArgs.add("-Xskip-metadata-version-check")
            }
        }
    }
}
