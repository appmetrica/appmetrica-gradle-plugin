package io.appmetrica.analytics.gradle

import java.util.Locale

fun String.uppercaseFirstChar(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
