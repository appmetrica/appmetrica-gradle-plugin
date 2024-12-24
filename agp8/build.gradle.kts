plugins {
    id("io.appmetrica.analytics.gradle.appmetrica-gradle-plugin-module")
}

val agpVersion = "8.2.2"

dependencies {
    compileOnly(project(":common"))
    compileOnly("com.android.tools.build:gradle:$agpVersion")
    testImplementation("com.android.tools.build:gradle:$agpVersion")
}
