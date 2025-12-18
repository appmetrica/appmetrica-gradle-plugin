package io.appmetrica.analytics.gradle.publishing

import io.appmetrica.analytics.gradle.uppercaseFirstChar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin

class PublishingPlugin : Plugin<Project> {

    companion object {
        const val MAVEN_LOCAL_REPOSITORY_NAME = "mavenLocal"
    }

    override fun apply(project: Project) {
        project.apply<MavenPublishPlugin>()
        project.apply<SigningPlugin>()

        project.configure<JavaPluginExtension> {
            withSourcesJar()
            withJavadocJar()
        }

        project.configurePom()
        project.configureSigning()

        project.registerPublishingTasks()
    }

    private fun Project.configurePom() {
        configure<PublishingExtension> {
            publications.withType<MavenPublication> publication@{
                pom {
                    name.set("AppMetrica Gradle Plugin")
                    description.set(project.description)

                    artifactId = "gradle"
                    url.set("https://appmetrica.io/")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://www.opensource.org/licenses/mit-license.php")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            name.set("AppMetrica")
                            url.set("https://appmetrica.io/")
                        }
                    }

                    scm {
                        connection.set("scm:git://github.com/appmetrica/appmetrica-gradle-plugin.git")
                        developerConnection.set("scm:git:git@github.com/appmetrica/appmetrica-gradle-plugin.git")
                        url.set("https://github.com/appmetrica/appmetrica-gradle-plugin.git")
                    }
                }
            }
        }
    }

    private fun Project.configureSigning() {
        fun hasSigningSecrets(): Boolean =
            properties["signing.keyId"] != null &&
                properties["signing.password"] != null &&
                properties["signing.secretKeyRingFile"] != null

        // prepare signing properties
        project.extra["signing.keyId"] = project.properties["signing.keyId"] ?: System.getenv("SIGNING_KEY_ID")
        project.extra["signing.password"] = project.properties["signing.password"] ?: System.getenv("SIGNING_PASSWORD")
        project.extra["signing.secretKeyRingFile"] = project.properties["signing.secretKeyRingFile"]
            ?: System.getenv("SIGNING_SECRET_KEY_RING_FILE")

        configure<SigningExtension> {
            sign(project.the<PublishingExtension>().publications)
        }
        tasks.withType<Sign> {
            required(hasSigningSecrets())
        }

        val checkSigningSecretsTask = tasks.register("checkSigningSecrets") {
            doLast {
                require(hasSigningSecrets()) {
                    "Not found all properties for signing artifacts"
                }
            }
        }
        configure<PublishingExtension> {
            repositories.configureEach repository@{
                if (name == MAVEN_LOCAL_REPOSITORY_NAME) return@repository // skip mavenLocal

                val capitalizedRepoName = name.uppercaseFirstChar()
                publications.configureEach {
                    val capitalizedPublicationName = name.uppercaseFirstChar()
                    afterEvaluate {
                        tasks.named(
                            "publish${capitalizedPublicationName}PublicationTo${capitalizedRepoName}Repository"
                        ) {
                            dependsOn(checkSigningSecretsTask)
                        }
                    }
                }
            }
        }
    }

    private fun Project.registerPublishingTasks() {
        createPublishTaskFor(MAVEN_LOCAL_REPOSITORY_NAME)

        configure<PublishingExtension> {
            repositories {
                configureEach { createPublishTaskFor(name) }
            }
        }
    }

    private fun Project.createPublishTaskFor(repositoryName: String) {
        val repoPrefix = when (repositoryName) {
            MAVEN_LOCAL_REPOSITORY_NAME -> MAVEN_LOCAL_REPOSITORY_NAME
            else -> "${repositoryName}Repository"
        }.uppercaseFirstChar()
        tasks.register("publishPluginsTo$repoPrefix") {
            group = "publishing"
            description = "Publish jar with code and all plugins to $repositoryName"

            dependsOn(tasks.named("publishPluginMavenPublicationTo$repoPrefix"))
            project.the<GradlePluginDevelopmentExtension>().plugins.configureEach {
                dependsOn(tasks.named("publish${name.uppercaseFirstChar()}PluginMarkerMavenPublicationTo$repoPrefix"))
            }
        }
    }
}
