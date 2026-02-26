package io.appmetrica.analytics.gradle.common.utils

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.testfixtures.ProjectBuilder
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

private class TestExtension(
    val enable: Property<Boolean>,
    val postApiKey: Property<String>,
    val soFiles: ConfigurableFileCollection
)

object ConfigResolverSpek : Spek({

    beforeGroup {
        Log.setLogger(ProjectBuilder.builder().build().logger)
    }

    describe("resolveFirstValue") {
        it("returns global value when set") {
            val project = ProjectBuilder.builder().build()
            val global = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java).apply { set("global-key") },
                soFiles = project.objects.fileCollection()
            )
            val resolver = ConfigResolver(
                extension = global,
                variantExtension = null,
                buildTypeExtension = null,
                flavorExtensions = null,
                providerFactory = project.providers,
                fileCollectionFactory = project::files
            )
            val result = resolver.resolveFirstValue(
                parameterName = "postApiKey",
                propertyExtractor = TestExtension::postApiKey,
                default = project.providers.provider { "default" }
            )
            assertThat(result.get()).isEqualTo("global-key")
        }

        it("returns variant value when global is not set") {
            val project = ProjectBuilder.builder().build()
            val global = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java),
                soFiles = project.objects.fileCollection()
            )
            val variant = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java).apply { set("variant-key") },
                soFiles = project.objects.fileCollection()
            )
            val resolver = ConfigResolver(
                extension = global,
                variantExtension = variant,
                buildTypeExtension = null,
                flavorExtensions = null,
                providerFactory = project.providers,
                fileCollectionFactory = project::files
            )
            val result = resolver.resolveFirstValue(
                parameterName = "postApiKey",
                propertyExtractor = TestExtension::postApiKey,
                default = project.providers.provider { "default" }
            )
            assertThat(result.get()).isEqualTo("variant-key")
        }

        it("returns buildType value when global and variant are not set") {
            val project = ProjectBuilder.builder().build()
            val empty = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java),
                soFiles = project.objects.fileCollection()
            )
            val buildType = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java).apply { set("bt-key") },
                soFiles = project.objects.fileCollection()
            )
            val resolver = ConfigResolver(
                extension = empty,
                variantExtension = empty,
                buildTypeExtension = buildType,
                flavorExtensions = null,
                providerFactory = project.providers,
                fileCollectionFactory = project::files
            )
            val result = resolver.resolveFirstValue(
                parameterName = "postApiKey",
                propertyExtractor = TestExtension::postApiKey,
                default = project.providers.provider { "default" }
            )
            assertThat(result.get()).isEqualTo("bt-key")
        }

        it("returns flavor value when higher sources are not set") {
            val project = ProjectBuilder.builder().build()
            val empty = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java),
                soFiles = project.objects.fileCollection()
            )
            val flavor = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java).apply { set("flavor-key") },
                soFiles = project.objects.fileCollection()
            )
            val resolver = ConfigResolver(
                extension = empty,
                variantExtension = null,
                buildTypeExtension = null,
                flavorExtensions = mapOf("prod" to flavor),
                providerFactory = project.providers,
                fileCollectionFactory = project::files
            )
            val result = resolver.resolveFirstValue(
                parameterName = "postApiKey",
                propertyExtractor = TestExtension::postApiKey,
                default = project.providers.provider { "default" }
            )
            assertThat(result.get()).isEqualTo("flavor-key")
        }

        it("returns default when nothing is set") {
            val project = ProjectBuilder.builder().build()
            val empty = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java),
                soFiles = project.objects.fileCollection()
            )
            val resolver = ConfigResolver(
                extension = empty,
                variantExtension = null,
                buildTypeExtension = null,
                flavorExtensions = null,
                providerFactory = project.providers,
                fileCollectionFactory = project::files
            )
            val result = resolver.resolveFirstValue(
                parameterName = "postApiKey",
                propertyExtractor = TestExtension::postApiKey,
                default = project.providers.provider { "default" }
            )
            assertThat(result.get()).isEqualTo("default")
        }

        it("global value has highest priority over all others") {
            val project = ProjectBuilder.builder().build()
            fun ext(value: String) = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java).apply { set(value) },
                soFiles = project.objects.fileCollection()
            )
            val resolver = ConfigResolver(
                extension = ext("global"),
                variantExtension = ext("variant"),
                buildTypeExtension = ext("buildType"),
                flavorExtensions = mapOf("prod" to ext("flavor")),
                providerFactory = project.providers,
                fileCollectionFactory = project::files
            )
            val result = resolver.resolveFirstValue(
                parameterName = "postApiKey",
                propertyExtractor = TestExtension::postApiKey,
                default = project.providers.provider { "default" }
            )
            assertThat(result.get()).isEqualTo("global")
        }
    }

    describe("resolveBoolean") {
        it("explicit false overrides explicit true from any source") {
            val project = ProjectBuilder.builder().build()
            val globalTrue = TestExtension(
                enable = project.objects.property(Boolean::class.java).apply { set(true) },
                postApiKey = project.objects.property(String::class.java),
                soFiles = project.objects.fileCollection()
            )
            val variantFalse = TestExtension(
                enable = project.objects.property(Boolean::class.java).apply { set(false) },
                postApiKey = project.objects.property(String::class.java),
                soFiles = project.objects.fileCollection()
            )
            val resolver = ConfigResolver(
                extension = globalTrue,
                variantExtension = variantFalse,
                buildTypeExtension = null,
                flavorExtensions = null,
                providerFactory = project.providers,
                fileCollectionFactory = project::files
            )
            val result = resolver.resolveBoolean(
                parameterName = "enable",
                propertyExtractor = TestExtension::enable,
                default = project.providers.provider { true }
            )
            assertThat(result.get()).isFalse()
        }

        it("explicit true wins when no false is set") {
            val project = ProjectBuilder.builder().build()
            val empty = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java),
                soFiles = project.objects.fileCollection()
            )
            val btTrue = TestExtension(
                enable = project.objects.property(Boolean::class.java).apply { set(true) },
                postApiKey = project.objects.property(String::class.java),
                soFiles = project.objects.fileCollection()
            )
            val resolver = ConfigResolver(
                extension = empty,
                variantExtension = null,
                buildTypeExtension = btTrue,
                flavorExtensions = null,
                providerFactory = project.providers,
                fileCollectionFactory = project::files
            )
            val result = resolver.resolveBoolean(
                parameterName = "enable",
                propertyExtractor = TestExtension::enable,
                default = project.providers.provider { false }
            )
            assertThat(result.get()).isTrue()
        }

        it("returns default when nothing is set") {
            val project = ProjectBuilder.builder().build()
            val empty = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java),
                soFiles = project.objects.fileCollection()
            )
            val resolver = ConfigResolver(
                extension = empty,
                variantExtension = null,
                buildTypeExtension = null,
                flavorExtensions = null,
                providerFactory = project.providers,
                fileCollectionFactory = project::files
            )
            val result = resolver.resolveBoolean(
                parameterName = "enable",
                propertyExtractor = TestExtension::enable,
                default = project.providers.provider { true }
            )
            assertThat(result.get()).isTrue()
        }

        it("false in flavor overrides true in global") {
            val project = ProjectBuilder.builder().build()
            val globalTrue = TestExtension(
                enable = project.objects.property(Boolean::class.java).apply { set(true) },
                postApiKey = project.objects.property(String::class.java),
                soFiles = project.objects.fileCollection()
            )
            val flavorFalse = TestExtension(
                enable = project.objects.property(Boolean::class.java).apply { set(false) },
                postApiKey = project.objects.property(String::class.java),
                soFiles = project.objects.fileCollection()
            )
            val resolver = ConfigResolver(
                extension = globalTrue,
                variantExtension = null,
                buildTypeExtension = null,
                flavorExtensions = mapOf("prod" to flavorFalse),
                providerFactory = project.providers,
                fileCollectionFactory = project::files
            )
            val result = resolver.resolveBoolean(
                parameterName = "enable",
                propertyExtractor = TestExtension::enable,
                default = project.providers.provider { true }
            )
            assertThat(result.get()).isFalse()
        }
    }

    describe("resolveUnique") {
        it("returns value when all sources agree") {
            val project = ProjectBuilder.builder().build()
            fun ext(value: String) = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java).apply { set(value) },
                soFiles = project.objects.fileCollection()
            )
            val resolver = ConfigResolver(
                extension = ext("same-key"),
                variantExtension = ext("same-key"),
                buildTypeExtension = null,
                flavorExtensions = null,
                providerFactory = project.providers,
                fileCollectionFactory = project::files
            )
            val result = resolver.resolveUnique(
                parameterName = "postApiKey",
                propertyExtractor = TestExtension::postApiKey,
                default = project.providers.provider { "default" }
            )
            assertThat(result.get()).isEqualTo("same-key")
        }

        it("returns default when sources conflict") {
            val project = ProjectBuilder.builder().build()
            fun ext(value: String) = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java).apply { set(value) },
                soFiles = project.objects.fileCollection()
            )
            val resolver = ConfigResolver(
                extension = ext("key-1"),
                variantExtension = ext("key-2"),
                buildTypeExtension = null,
                flavorExtensions = null,
                providerFactory = project.providers,
                fileCollectionFactory = project::files
            )
            val result = resolver.resolveUnique(
                parameterName = "postApiKey",
                propertyExtractor = TestExtension::postApiKey,
                default = project.providers.provider { "default" }
            )
            assertThat(result.get()).isEqualTo("default")
        }

        it("returns default when nothing is set") {
            val project = ProjectBuilder.builder().build()
            val empty = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java),
                soFiles = project.objects.fileCollection()
            )
            val resolver = ConfigResolver(
                extension = empty,
                variantExtension = null,
                buildTypeExtension = null,
                flavorExtensions = null,
                providerFactory = project.providers,
                fileCollectionFactory = project::files
            )
            val result = resolver.resolveUnique(
                parameterName = "postApiKey",
                propertyExtractor = TestExtension::postApiKey,
                default = project.providers.provider { "default" }
            )
            assertThat(result.get()).isEqualTo("default")
        }
    }

    describe("resolveMerged") {
        it("returns default when nothing is configured") {
            val project = ProjectBuilder.builder().build()
            val empty = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java),
                soFiles = project.objects.fileCollection()
            )
            val defaultFile = project.file("default.so")
            defaultFile.createNewFile()
            val resolver = ConfigResolver(
                extension = empty,
                variantExtension = null,
                buildTypeExtension = null,
                flavorExtensions = null,
                providerFactory = project.providers,
                fileCollectionFactory = project::files
            )
            val result = resolver.resolveMerged(
                parameterName = "soFiles",
                propertyExtractor = TestExtension::soFiles,
                default = project.files(defaultFile)
            )
            assertThat(result.files).containsExactly(defaultFile)
        }

        it("merges files from multiple sources") {
            val project = ProjectBuilder.builder().build()
            val file1 = project.file("file1.so").apply { createNewFile() }
            val file2 = project.file("file2.so").apply { createNewFile() }

            val global = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java),
                soFiles = project.objects.fileCollection().from(file1)
            )
            val variant = TestExtension(
                enable = project.objects.property(Boolean::class.java),
                postApiKey = project.objects.property(String::class.java),
                soFiles = project.objects.fileCollection().from(file2)
            )
            val resolver = ConfigResolver(
                extension = global,
                variantExtension = variant,
                buildTypeExtension = null,
                flavorExtensions = null,
                providerFactory = project.providers,
                fileCollectionFactory = project::files
            )
            val result = resolver.resolveMerged(
                parameterName = "soFiles",
                propertyExtractor = TestExtension::soFiles,
                default = project.files()
            )
            assertThat(result.files).containsExactlyInAnyOrder(file1, file2)
        }
    }
})
