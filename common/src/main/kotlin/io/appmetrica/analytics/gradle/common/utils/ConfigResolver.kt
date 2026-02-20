package io.appmetrica.analytics.gradle.common.utils

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import kotlin.reflect.KProperty1

/**
 * Resolves configuration values based on priority:
 * global > variant > buildType > first flavor > default
 *
 * For enable properties uses special logic:
 * explicit false (in any source) > explicit true (in any source) > default
 *
 * @param extension The main AppMetrica extension
 * @param variantExtension AppMetrica extension for the specific variant
 * @param buildTypeExtension AppMetrica extension provider for BuildType
 * @param flavorExtensions AppMetrica extension provider for Flavors
 * @param providerFactory Gradle provider factory for creating lazy providers
 * @param fileCollectionFactory Factory for creating file collections
 */
class ConfigResolver<EXTENSION>(
    private val extension: EXTENSION,
    private val variantExtension: EXTENSION?,
    private val buildTypeExtension: EXTENSION?,
    private val flavorExtensions: Map<String, EXTENSION>?,
    private val providerFactory: ProviderFactory,
    private val fileCollectionFactory: (Any) -> FileCollection
) {

    fun <T : Any> resolveFirstValue(
        parameterName: String,
        propertyExtractor: KProperty1<EXTENSION, Provider<T>>,
        default: Provider<T>
    ): Provider<T> {
        val sources = buildList {
            add("extension" to propertyExtractor(extension))
            variantExtension?.let { add("variantExtension" to propertyExtractor(it)) }
            buildTypeExtension?.let { add("buildTypeExtension" to propertyExtractor(it)) }
            flavorExtensions?.forEach { (flavorName, flavorExtension) ->
                add("${flavorName}FlavorExtension" to propertyExtractor(flavorExtension))
            }
        }

        var result: Provider<T> = default.map { value ->
            Log.info("Using default value `$value` for $parameterName")
            value
        }
        for ((sourceName, provider) in sources.reversed()) {
            result = provider.map { value ->
                Log.info("Using value `$value` from $sourceName for $parameterName")
                value
            }.orElse(result)
        }
        return result
    }

    fun resolveBoolean(
        parameterName: String,
        propertyExtractor: KProperty1<EXTENSION, Provider<Boolean>>,
        default: Provider<Boolean>
    ): Provider<Boolean> {
        return providerFactory.provider {
            val allValues = buildMap {
                put("extension", propertyExtractor(extension).orNull)
                put("variantExtension", variantExtension?.let { propertyExtractor(it).orNull })
                put("buildTypeExtension", buildTypeExtension?.let { propertyExtractor(it).orNull })
                flavorExtensions?.forEach { (flavorName, flavorExtension) ->
                    put("${flavorName}FlavorExtension", propertyExtractor(flavorExtension).orNull)
                }
            }

            val firstFalse = allValues.entries.firstOrNull { it.value == false }
            val firstTrue = allValues.entries.firstOrNull { it.value == true }

            when {
                firstFalse != null -> {
                    Log.info("Using value `false` from ${firstFalse.key} for $parameterName")
                    false
                }
                firstTrue != null -> {
                    Log.info("Using value `true` from ${firstTrue.key} for $parameterName")
                    true
                }
                else -> {
                    Log.info("Using default value `${default.orNull}` for $parameterName")
                    default.orNull
                }
            }
        }
    }

    fun resolveMerged(
        parameterName: String,
        propertyExtractor: KProperty1<EXTENSION, ConfigurableFileCollection>,
        default: FileCollection
    ): FileCollection {
        return fileCollectionFactory(
            providerFactory.provider {
                val configured = buildList<FileCollection> {
                    propertyExtractor(extension).takeIf { it.isConfigured() }?.let {
                        Log.info("Adding files from extension for $parameterName: ${it.files}")
                        add(it)
                    }
                    variantExtension?.let {
                        propertyExtractor(it).takeIf { collection -> collection.isConfigured() }?.let {
                            Log.info("Adding files from variantExtension for $parameterName: ${it.files}")
                            add(it)
                        }
                    }
                    buildTypeExtension?.let {
                        propertyExtractor(it).takeIf { collection -> collection.isConfigured() }?.let {
                            Log.info("Adding files from buildTypeExtension for $parameterName: ${it.files}")
                            add(it)
                        }
                    }
                    flavorExtensions?.forEach { (flavorName, flavorExtension) ->
                        propertyExtractor(flavorExtension).takeIf { it.isConfigured() }?.let {
                            Log.info("Adding files from ${flavorName}FlavorExtension for $parameterName: ${it.files}")
                            add(it)
                        }
                    }
                }
                if (configured.isNotEmpty()) {
                    configured.reduce { acc, files -> acc + files }
                } else {
                    Log.info("Using default value for $parameterName: ${default.files}")
                    default
                }
            }
        )
    }

    fun <T> resolveUnique(
        parameterName: String,
        propertyExtractor: KProperty1<EXTENSION, Provider<T>>,
        default: Provider<T>
    ): Provider<T> {
        return providerFactory.provider {
            val allValues = buildMap {
                propertyExtractor(extension).orNull?.let {
                    put("extension", it)
                }
                variantExtension?.let {
                    propertyExtractor(it).orNull?.let { value ->
                        put("variantExtension", value)
                    }
                }
                buildTypeExtension?.let {
                    propertyExtractor(it).orNull?.let { value ->
                        put("buildTypeExtension", value)
                    }
                }
                flavorExtensions?.forEach { (flavorName, flavorExtension) ->
                    propertyExtractor(flavorExtension).orNull?.let { value ->
                        put("${flavorName}FlavorExtension", value)
                    }
                }
            }

            val distinctValues = allValues.values.distinct()
            when {
                distinctValues.isEmpty() -> {
                    Log.info("Using default value `${default.orNull}` for $parameterName")
                    default.orNull
                }
                distinctValues.size == 1 -> {
                    Log.info("Using unique value `${distinctValues.first()}` for $parameterName")
                    distinctValues.first()
                }
                else -> {
                    Log.error(
                        "Conflicting values for $parameterName: " +
                            allValues.entries.joinToString { "${it.key}=`${it.value}`" } +
                            ". Using default value `${default.orNull}`"
                    )
                    default.orNull
                }
            }
        }
    }

    private fun ConfigurableFileCollection.isConfigured(): Boolean = from.isNotEmpty()
}
