package io.appmetrica.analytics.gradle

import io.appmetrica.gradle.common.plugins.CodeQualityPlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
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

        project.dependencies {
            val implementation by project.configurations.getting
            val testImplementation by project.configurations.getting
            val testRuntimeOnly by project.configurations.getting

            implementation(localGroovy())
            implementation(gradleApi())

            implementation("io.github.http-builder-ng:http-builder-ng-apache:1.0.3")
            implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.21")
            implementation("com.google.code.gson:gson:2.8.5")

            testImplementation("junit:junit:4.12")
            testImplementation("org.assertj:assertj-core:3.11.1")
            testImplementation("com.nhaarman:mockito-kotlin:0.9.0")
            testImplementation("org.mockito:mockito-core:2.2.9")

            testImplementation("org.spekframework.spek2:spek-dsl-jvm:2.0.1")
            testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:2.0.1")
            testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.6.21")
            testImplementation("org.mock-server:mockserver-netty:5.11.1")
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
