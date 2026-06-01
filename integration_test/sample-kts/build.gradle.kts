plugins {
    id("com.android.application") apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("io.appmetrica.analytics") version "2.1.0" apply false
}

tasks.named<Wrapper>("wrapper") {
    distributionUrl = "https://services.gradle.org/distributions/gradle-${project.properties["wrapper.version"]}.zip"
}
