plugins {
    id("io.appmetrica.analytics.gradle.appmetrica-gradle-plugin-module")
}

dependencies {
    compileOnly(project(":common"))
    compileOnly(appMetricaGradlePluginLibs.agp7)
    testImplementation(appMetricaGradlePluginLibs.agp7)
}
