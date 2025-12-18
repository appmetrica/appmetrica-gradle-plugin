plugins {
    id("io.appmetrica.analytics.gradle.appmetrica-gradle-plugin-module")
}

dependencies {
    compileOnly(project(":common"))
    compileOnly(libs.agp7)
    testImplementation(libs.agp7)
}
