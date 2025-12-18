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
    implementation(libs.kotlin.gradlePlugin)
    // https://detekt.dev/docs/gettingstarted/gradle/
    implementation(libs.detekt.gradlePlugin)
    // by source
    implementation(libs.appMetricaCommon)
    implementation(libs.appMetricaMavenCentralPublish)
}
