package io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.reader

import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DebugAbbrevAttribute
import java.io.IOException

interface AttributesReader<T> {

    @Throws(IOException::class)
    fun readAttributes(attributes: List<DebugAbbrevAttribute>): T
}
