if (file("internal.settings.gradle.kts").exists()) {
    apply(from = "internal.settings.gradle.kts")
} else {
    apply(from = "public.settings.gradle.kts")
}

rootProject.name = "appmetrica-gradle-plugin"

// build scripts
includeBuild("build_logic")

// modules
include("agp7")
include("agp8")
include("common")
include("main")
include("rtm-dummy")
