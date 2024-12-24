plugins {
    id("io.appmetrica.analytics.gradle.appmetrica-gradle-plugin-module")
}

val agpVersion = "4.2.0"

dependencies {
    compileOnly(project(":common"))
    compileOnly("com.android.tools.build:gradle:$agpVersion")
    testImplementation("com.android.tools.build:gradle:$agpVersion")
}
