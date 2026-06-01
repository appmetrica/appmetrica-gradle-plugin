import io.appmetrica.analytics.gradle.agp8.appmetrica
import org.gradle.kotlin.dsl.support.kotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("io.appmetrica.analytics")
}

android {
    namespace = "io.appmetrica.analytics.gradle.plugin.sample.kts"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "io.appmetrica.analytics.gradle.plugin.sample.kts"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            appmetrica {
                enable.set(true)
                offline.set(true)
                postApiKey.set("kts-release-key")
            }
        }
        debug {
            appmetrica {
                enable.set(false)
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
}

dependencies {
    implementation("io.appmetrica.analytics:analytics:8.2.0")
}
