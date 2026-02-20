package io.appmetrica.analytics.gradle.common.config

import io.appmetrica.analytics.gradle.common.api.AndroidApplicationVariant
import io.appmetrica.analytics.gradle.common.extension.AppMetricaExtension
import io.appmetrica.analytics.gradle.common.extension.AppMetricaNdkExtension
import io.appmetrica.analytics.gradle.common.utils.ConfigResolver
import org.gradle.api.Project

class ConfigFactory(
    project: Project,
    extension: AppMetricaExtension,
    variantExtension: AppMetricaExtension?,
    buildTypeExtension: AppMetricaExtension?,
    flavorExtensions: Map<String, AppMetricaExtension>,
    variant: AndroidApplicationVariant
) {

    private val defaultEnable = variant.buildType.map { it == "release" }.orElse(false)
    private val defaultPostApiKey = project.providers.provider { "" }
    private val defaultOffline = project.providers.provider { false }
    private val defaultAllowTwoAppMetricas = project.providers.provider { false }
    private val defaultEnableAnalytics = project.providers.provider { true }
    private val defaultNdkEnable = project.providers.provider { false }
    private val defaultAddNdkCrashesDependency = project.providers.provider { true }
    private val defaultAdditionalSoFiles = project.files()

    private val configResolver = ConfigResolver(
        extension,
        variantExtension,
        buildTypeExtension,
        flavorExtensions,
        project.providers,
        project::files
    )

    private val ndkConfigResolver = ConfigResolver(
        extension.ndk,
        variantExtension?.ndk,
        buildTypeExtension?.ndk,
        flavorExtensions.mapValues { it.value.ndk },
        project.providers,
        project::files
    )

    val appMetricaConfig: AppMetricaPluginConfig by lazy {
        AppMetricaPluginConfig(
            enable = configResolver.resolveBoolean(
                parameterName = "enable",
                propertyExtractor = AppMetricaExtension::enable,
                default = defaultEnable
            ),
            postApiKey = configResolver.resolveUnique(
                parameterName = "postApiKey",
                propertyExtractor = AppMetricaExtension::postApiKey,
                default = defaultPostApiKey
            ),
            offline = configResolver.resolveBoolean(
                parameterName = "offline",
                propertyExtractor = AppMetricaExtension::offline,
                default = defaultOffline
            ),
            mappingFile = configResolver.resolveFirstValue(
                parameterName = "mappingFile",
                propertyExtractor = AppMetricaExtension::mappingFile,
                default = variant.mappingFile
            ),
            allowTwoAppMetricas = configResolver.resolveBoolean(
                parameterName = "allowTwoAppMetricas",
                propertyExtractor = AppMetricaExtension::allowTwoAppMetricas,
                default = defaultAllowTwoAppMetricas
            ),
            enableAnalytics = configResolver.resolveBoolean(
                parameterName = "enableAnalytics",
                propertyExtractor = AppMetricaExtension::enableAnalytics,
                default = defaultEnableAnalytics
            ),
            ndk = AppMetricaPluginNdkConfig(
                enable = ndkConfigResolver.resolveBoolean(
                    parameterName = "ndkEnable",
                    propertyExtractor = AppMetricaNdkExtension::enable,
                    default = defaultNdkEnable
                ),
                soFiles = ndkConfigResolver.resolveMerged(
                    parameterName = "ndkSoFiles",
                    propertyExtractor = AppMetricaNdkExtension::soFiles,
                    default = variant.soFiles
                ),
                additionalSoFiles = ndkConfigResolver.resolveMerged(
                    parameterName = "ndkAdditionalSoFiles",
                    propertyExtractor = AppMetricaNdkExtension::additionalSoFiles,
                    default = defaultAdditionalSoFiles
                ),
                addNdkCrashesDependency = ndkConfigResolver.resolveBoolean(
                    parameterName = "ndkAddNdkCrashesDependency",
                    propertyExtractor = AppMetricaNdkExtension::addNdkCrashesDependency,
                    default = defaultAddNdkCrashesDependency
                )
            )
        )
    }
}
