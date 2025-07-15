plugins {
    id("io.appmetrica.analytics.gradle.appmetrica-gradle-plugin-module")
}

dependencies {
    if (rootProject.file("internal.settings.gradle.kts").exists()) {
        embed("com.yandex.browser:rtm:2.1.0")
    } else {
        embed(project(":rtm-dummy"))
    }
    compileOnly("com.android.tools:common:27.2.0")
}
