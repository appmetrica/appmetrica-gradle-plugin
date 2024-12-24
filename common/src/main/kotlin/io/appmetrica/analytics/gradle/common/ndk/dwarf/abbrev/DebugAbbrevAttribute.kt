package io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev

import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

class DebugAbbrevAttribute(
    val name: DWAttribute,
    val form: DWForm
)

@Throws(IOException::class)
fun ByteReader.readDebugAbbrevAttributes(): List<DebugAbbrevAttribute> {
    val attributes = mutableListOf<DebugAbbrevAttribute>()
    while (readDebugAbbrevAttribute()?.also { attributes.add(it) } != null);
    return attributes
}

@Throws(IOException::class)
private fun ByteReader.readDebugAbbrevAttribute(): DebugAbbrevAttribute? {
    val name = readULEB128()
    val form = readULEB128()
    return if (name == 0 && form == 0) {
        null
    } else {
        DebugAbbrevAttribute(
            DWAttribute.fromValue(name),
            DWForm.fromValue(form)
        )
    }
}
