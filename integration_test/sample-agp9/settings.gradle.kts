if (file("internal.settings.gradle.kts").exists()) {
    apply(from = "internal.settings.gradle.kts")
} else {
    apply(from = "public.settings.gradle.kts")
}

rootProject.name = "sample-agp9"

include(":app")
