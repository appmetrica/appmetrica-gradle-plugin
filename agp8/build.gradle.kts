plugins {
    id("io.appmetrica.analytics.gradle.appmetrica-gradle-plugin-module")
}

dependencies {
    compileOnly(project(":common"))
    compileOnly(libs.agp8)
    testImplementation(libs.agp8)
}
