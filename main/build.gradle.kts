plugins {
    `java-gradle-plugin`
    id("io.appmetrica.analytics.gradle.appmetrica-gradle-plugin-module")
    id("io.appmetrica.analytics.gradle.appmetrica-gradle-plugin-publish")
    id("io.appmetrica.analytics.gradle.appmetrica-gradle-plugin-public-publish")
}

description = "Plugin for uploading mappings and symbols during an application build process."

gradlePlugin {
    website = "https://github.com/appmetrica/appmetrica-gradle-plugin"
    vcsUrl = "https://github.com/appmetrica/appmetrica-gradle-plugin.git"
    plugins {
        create("appMetricaGradlePlugin") {
            id = "io.appmetrica.analytics"
            implementationClass = "io.appmetrica.analytics.gradle.plugin.AppMetricaPlugin"
            displayName = "AppMetrica Gradle Plugin"
            description = project.description
            tags = listOf("appmetrica")
        }
    }
}

dependencies {
    embed(project(":agp4"))
    embed(project(":agp7"))
    embed(project(":agp8"))
    embed(project(":common"))
}
