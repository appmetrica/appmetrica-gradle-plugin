package io.appmetrica.analytics.gradle.publishing

import io.appmetrica.gradle.publishing.MavenCentralPublishExtension
import io.appmetrica.gradle.publishing.MavenCentralPublishPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class PublicPublishPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.apply<MavenCentralPublishPlugin>()

        project.afterEvaluate {
            project.plugins.withType<MavenPublishPlugin> {
                project.configure<MavenCentralPublishExtension> {
                    publishToMavenLocalTask.set(project.tasks.named("publishPluginsToMavenLocal"))
                    artifactIds.set(
                        project.extensions.findByType(PublishingExtension::class.java)!!
                            .publications
                            .withType<MavenPublication>()
                            .map { it.artifactId }
                    )
                }
            }
        }
    }
}
