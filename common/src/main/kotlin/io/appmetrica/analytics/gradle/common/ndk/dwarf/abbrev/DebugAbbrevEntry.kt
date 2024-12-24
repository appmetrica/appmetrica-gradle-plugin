package io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev

import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

class DebugAbbrevEntry(
    val number: Int,
    val tag: DWTag,
    val hasChildren: Boolean,
    val attributes: List<DebugAbbrevAttribute>
)

@Throws(IOException::class)
fun ByteReader.readDebugAbbrevEntries(): Map<Int, DebugAbbrevEntry> {
    val entries = mutableMapOf<Int, DebugAbbrevEntry>()
    while (readDebugAbbrevEntry()?.also { entries[it.number] = it } != null);
    return entries
}

@Throws(IOException::class)
private fun ByteReader.readDebugAbbrevEntry(): DebugAbbrevEntry? {
    val number = readULEB128()
    return if (number == 0) {
        null
    } else {
        DebugAbbrevEntry(
            number = number,
            tag = DWTag.fromValue(readULEB128()),
            hasChildren = readInt(Byte.SIZE_BYTES) != 0,
            attributes = readDebugAbbrevAttributes()
        )
    }
}
