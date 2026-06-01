package io.appmetrica.analytics.gradle.agp8

import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.api.dsl.ApplicationProductFlavor
import io.appmetrica.analytics.gradle.common.APPMETRICA_PLUGIN
import io.appmetrica.analytics.gradle.common.extension.AppMetricaExtension
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware

/* ktlint-disable appmetrica-rules:no-top-level-members */
fun ApplicationBuildType.appmetrica(action: Action<AppMetricaExtension>) {
    (this as ExtensionAware).extensions.configure(APPMETRICA_PLUGIN, action)
}

fun ApplicationProductFlavor.appmetrica(action: Action<AppMetricaExtension>) {
    (this as ExtensionAware).extensions.configure(APPMETRICA_PLUGIN, action)
}
/* ktlint-enable appmetrica-rules:no-top-level-members */
