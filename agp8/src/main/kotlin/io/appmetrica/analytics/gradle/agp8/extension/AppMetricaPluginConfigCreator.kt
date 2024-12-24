package io.appmetrica.analytics.gradle.agp8.extension

import com.android.build.api.variant.ApplicationVariant
import io.appmetrica.analytics.gradle.common.config.AppMetricaPluginConfig
import io.appmetrica.analytics.gradle.common.config.AppMetricaPluginNdkConfig

class AppMetricaPluginConfigCreator(
    private val extension: AppMetricaExtension
) {

    fun create(variant: ApplicationVariant): AppMetricaPluginConfig {
        return AppMetricaPluginConfig(
            enable = { extension.enable.invoke(variant) },
            postApiKey = { extension.postApiKey.invoke(variant) },
            offline = { extension.offline.invoke(variant) },
            mappingFile = { extension.mappingFile?.invoke(variant) },
            enableAnalytics = extension.enableAnalytics,
            allowTwoAppMetricas = { extension.allowTwoAppMetricas.invoke(variant) },
            ndk = AppMetricaPluginNdkConfig(
                enable = { extension.ndk.enable.invoke(variant) },
                soFiles = { extension.ndk.soFiles.invoke(variant) },
                additionalSoFiles = { extension.ndk.additionalSoFiles.invoke(variant) },
                addNdkCrashesDependency = { extension.ndk.addNdkCrashesDependency.invoke(variant) }
            )
        )
    }
}
