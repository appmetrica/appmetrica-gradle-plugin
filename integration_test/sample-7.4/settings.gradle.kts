if (file("internal.settings.gradle.kts").exists()) {
    apply(from = "internal.settings.gradle.kts")
} else {
    apply(from = "public.settings.gradle.kts")
}

rootProject.name = "sample-7.4"

include(":app")
include(":module")
