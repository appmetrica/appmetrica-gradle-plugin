plugins {
    id("io.appmetrica.analytics.gradle.appmetrica-gradle-plugin-module")
}

dependencies {
    if (rootProject.file("internal.settings.gradle.kts").exists()) {
        embed(appMetricaGradlePluginLibs.rtm)
    } else {
        embed(project(":rtm-dummy"))
    }
    compileOnly(appMetricaGradlePluginLibs.android.tools.common)
}
