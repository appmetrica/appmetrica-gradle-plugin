plugins {
    id("io.appmetrica.analytics.gradle.appmetrica-gradle-plugin-module")
}

dependencies {
    if (rootProject.file("internal.settings.gradle.kts").exists()) {
        embed(libs.rtm)
    } else {
        embed(project(":rtm-dummy"))
    }
    compileOnly(libs.android.tools.common)
}
