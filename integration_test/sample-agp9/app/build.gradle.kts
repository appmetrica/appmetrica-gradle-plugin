import io.appmetrica.analytics.gradle.agp8.appmetrica
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("io.appmetrica.analytics")
}

android {
    namespace = "io.appmetrica.analytics.gradle.plugin.sample.agp9"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "io.appmetrica.analytics.gradle.plugin.sample.agp9"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {}
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            appmetrica {
                postApiKey.set("agp9-release-key")
            }
        }
    }

    flavorDimensions += listOf("first", "second")
    productFlavors {
        create("firstOne") {
            dimension = "first"
        }
        create("firstTwo") {
            dimension = "first"
            appmetrica {
                enable.set(false)
            }
        }
        create("secondOne") {
            dimension = "second"
        }
        create("secondTwo") {
            dimension = "second"
            appmetrica {
                enable.set(true)
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    ndkVersion = "21.1.6352462"
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.10.2"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

appmetrica {
    enable.set(true)
    offline.set(true)
    ndk {
        enable.set(true)
    }
}

dependencies {
    implementation("io.appmetrica.analytics:analytics:8.2.0")
}
