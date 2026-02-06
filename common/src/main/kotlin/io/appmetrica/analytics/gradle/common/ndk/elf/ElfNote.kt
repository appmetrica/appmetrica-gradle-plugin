package io.appmetrica.analytics.gradle.common.ndk.elf

import io.appmetrica.analytics.gradle.common.Log
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

class ElfNote(
    val nameSize: Int,
    val descSize: Int,
    val type: Int,
    val name: String,
    val desc: ByteArray
)

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Throws(IOException::class)
fun parseElfNotes(reader: ByteReader, noteSection: ElfSectionHeader): List<ElfNote> =
    parseElfNotes(reader, noteSection.offset, noteSection.size)

@Throws(IOException::class)
fun parseElfNotes(reader: ByteReader, noteSection: ElfProgramHeader): List<ElfNote> =
    parseElfNotes(reader, noteSection.offset, noteSection.fileSize)

@Throws(IOException::class)
fun parseElfNotes(reader: ByteReader, offset: Long, size: Long): List<ElfNote> {
    val notes = mutableListOf<ElfNote>()
    reader.seek(offset)
    val endOffset = offset + size
    while (reader.getCurrentOffset() < endOffset) {
        Log.debug("offset = ${reader.getCurrentOffset()}, endOffset = $endOffset")
        notes.add(reader.readElfNote())
    }
    return notes
}

@Throws(IOException::class)
fun ByteReader.readElfNote(): ElfNote {
    val nameSize = readInt(Int.SIZE_BYTES)
    val descSize = readInt(Int.SIZE_BYTES)
    val type = readInt(Int.SIZE_BYTES)
    val name = readNullTerminatedString(Charsets.UTF_8)
    readBytes((Int.SIZE_BYTES - nameSize % Int.SIZE_BYTES) % Int.SIZE_BYTES)
    val desc = readBytes(descSize)
    readBytes((Int.SIZE_BYTES - nameSize % Int.SIZE_BYTES) % Int.SIZE_BYTES)
    Log.debug("nameSize = $nameSize, descSize = $descSize, type = $type, name = $name, desc = $desc")
    return ElfNote(nameSize, descSize, type, name, desc)
}
/* ktlint-enable appmetrica-rules:no-top-level-members */
