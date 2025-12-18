package io.appmetrica.analytics.gradle

import io.appmetrica.gradle.common.plugins.CodeQualityPlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.VersionCatalogsExtension
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
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class GradlePluginModule : Plugin<Project> {

    override fun apply(project: Project) {
        project.apply<KotlinPluginWrapper>() // kotlin
        project.apply<CodeQualityPlugin>() // id("appmetrica-codequality")
        project.apply<JacocoPlugin>() // jacoco

        project.group = Constants.Library.group
        project.version = Constants.Library.versionName + (project.properties["versionPostfix"] ?: "")

        project.createEmbedConfiguration()
        project.configureTests()
        project.configureJacoco()
        project.configureJavaVersion()
        project.configureKotlinVersion()

        val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

        project.dependencies {
            val implementation by project.configurations.getting
            val testImplementation by project.configurations.getting
            val testRuntimeOnly by project.configurations.getting

            implementation(localGroovy())
            implementation(gradleApi())

            implementation(libs.findLibrary("httpBuilder-apache").get())
            implementation(libs.findLibrary("kotlin-stdlib").get())
            implementation(libs.findLibrary("gson").get())

            testImplementation(libs.findLibrary("junit").get())
            testImplementation(libs.findLibrary("assertj").get())
            testImplementation(libs.findLibrary("mockito-kotlin").get())
            testImplementation(libs.findLibrary("mockito-core").get())

            testImplementation(libs.findLibrary("spek-dsl").get())
            testRuntimeOnly(libs.findLibrary("spek-runner").get())
            testRuntimeOnly(libs.findLibrary("kotlin-reflect").get())
            testImplementation(libs.findLibrary("mockserver").get())
        }
    }

    private fun Project.createEmbedConfiguration() {
        val embed: Configuration by project.configurations.creating

        project.configurations.named("compileOnly") {
            extendsFrom(embed)
        }

        project.tasks.named<Jar>("jar") {
            // embed all root artifacts without dependencies
            val embeddedDependencies = embed.resolvedConfiguration.firstLevelModuleDependencies
            embeddedDependencies.forEach { dependency ->
                rootProject.subprojects {
                    if (name in dependency.module.id.toString()) {
                        this@named.dependsOn(tasks.named<Jar>("jar"))
                    }
                }
            }
            from(embeddedDependencies.map { dependency ->
                dependency.moduleArtifacts.map { zipTree(it.file.canonicalFile) }
            })
        }
    }

    private fun Project.configureTests() {
        tasks.named<Test>("test") {
            useJUnitPlatform {
                includeEngines("spek2")
            }
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
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_11.toString()
                freeCompilerArgs += "-Xskip-metadata-version-check"
            }
        }
    }
}
