package io.appmetrica.analytics.gradle.agp8.extension

import com.android.build.api.variant.ApplicationVariant
import java.io.File

open class AppMetricaNdkExtension {

    var enable: (ApplicationVariant) -> Boolean = { _ -> false }
    var soFiles: (ApplicationVariant) -> List<File> = { emptyList() }
    var additionalSoFiles: (ApplicationVariant) -> List<File> = { emptyList() }
    var addNdkCrashesDependency: (ApplicationVariant) -> Boolean = { true }
}
