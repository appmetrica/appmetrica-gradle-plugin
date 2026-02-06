package io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev

import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Throws(IOException::class)
fun parseAbbrevSection(reader: ByteReader, abbrevOffset: Long): Map<Int, DebugAbbrevEntry> {
    val entriesOffset = reader.getCurrentOffset()
    reader.seek(abbrevOffset)
    val debugAbbrevEntries = reader.readDebugAbbrevEntries()
    reader.seek(entriesOffset)
    return debugAbbrevEntries
}
/* ktlint-enable appmetrica-rules:no-top-level-members */
