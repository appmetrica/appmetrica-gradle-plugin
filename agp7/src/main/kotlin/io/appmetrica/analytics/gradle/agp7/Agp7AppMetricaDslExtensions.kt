package io.appmetrica.analytics.gradle.agp7

import com.android.build.gradle.internal.dsl.BuildType
import com.android.build.gradle.internal.dsl.ProductFlavor
import io.appmetrica.analytics.gradle.common.APPMETRICA_PLUGIN
import io.appmetrica.analytics.gradle.common.extension.AppMetricaExtension
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware

/* ktlint-disable appmetrica-rules:no-top-level-members */
fun BuildType.appmetrica(action: Action<AppMetricaExtension>) {
    (this as ExtensionAware).extensions.configure(APPMETRICA_PLUGIN, action)
}

fun ProductFlavor.appmetrica(action: Action<AppMetricaExtension>) {
    (this as ExtensionAware).extensions.configure(APPMETRICA_PLUGIN, action)
}
/* ktlint-enable appmetrica-rules:no-top-level-members */
