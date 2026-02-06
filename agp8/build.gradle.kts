plugins {
    id("io.appmetrica.analytics.gradle.appmetrica-gradle-plugin-module")
}

dependencies {
    compileOnly(project(":common"))
    compileOnly(appMetricaGradlePluginLibs.agp8)
    testImplementation(appMetricaGradlePluginLibs.agp8)
}
