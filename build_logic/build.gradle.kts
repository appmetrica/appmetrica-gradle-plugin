import io.appmetrica.gradle.extensions.plugin

@Suppress("DSL_SCOPE_VIOLATION") // https://youtrack.jetbrains.com/issue/KTIJ-19369 fixed in gradle 8.1
plugins {
    alias(libs.plugins.appMetricaGradlePlugin)
}

group = "io.appmetrica.analytics.gradle"

gradlePlugin {
    plugin("appmetrica-gradle-plugin-module", "io.appmetrica.analytics.gradle.GradlePluginModule")
    plugin("appmetrica-gradle-plugin-publish", "io.appmetrica.analytics.gradle.publishing.PublishingPlugin")
    plugin("appmetrica-gradle-plugin-public-publish", "io.appmetrica.analytics.gradle.publishing.PublicPublishPlugin")
}

dependencies {
    // https://kotlinlang.org/docs/gradle.html
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21")
    // https://detekt.dev/docs/gettingstarted/gradle/
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.3")
    // by source
    implementation(libs.appMetricaCommon)
    implementation(libs.appMetricaMavenCentralPublish)
}
