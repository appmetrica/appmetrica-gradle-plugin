package io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.reader

import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DebugAbbrevAttribute
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.skipDebugForm
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.CompilationUnitHeader
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

class SkipAttributesReader(
    private val reader: ByteReader,
    private val cuHeader: CompilationUnitHeader
) : AttributesReader<Unit> {

    @Throws(IOException::class)
    override fun readAttributes(attributes: List<DebugAbbrevAttribute>) {
        attributes.forEach { skipDebugInfoEntryAttribute(it) }
    }

    @Throws(IOException::class)
    private fun skipDebugInfoEntryAttribute(attribute: DebugAbbrevAttribute) {
        reader.skipDebugForm(attribute.form, cuHeader)
    }
}
