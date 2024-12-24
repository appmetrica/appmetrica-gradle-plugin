package io.appmetrica.analytics.gradle.common.ndk.elf

import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException
import kotlin.experimental.xor
import kotlin.math.min

private const val GNU_BUILD_ID_SIZE = 16
private const val NT_GNU_BUILD_ID = 0x3
private const val MAX_PAGE_SIZE = 4096L

@Throws(IOException::class)
fun parseElfBuildId(
    reader: ByteReader,
    sectionHeaders: ElfSectionHeaders,
    programHeaders: ElfProgramHeaders
): ByteArray? {
    parseElfBuildIdNote(reader, sectionHeaders, programHeaders)?.let { return it }
    return hashElfTextSection(reader, sectionHeaders)
}

@Throws(IOException::class)
private fun parseElfBuildIdNote(
    reader: ByteReader,
    sectionHeaders: ElfSectionHeaders,
    programHeaders: ElfProgramHeaders
): ByteArray? {
    parseElfBuildIdNoteFromProgramHeader(reader, programHeaders)?.let { return it }
    return parseElfBuildIdNoteFromSectionHeader(reader, sectionHeaders)
}

@Throws(IOException::class)
private fun parseElfBuildIdNoteFromProgramHeader(reader: ByteReader, headers: ElfProgramHeaders): ByteArray? {
    headers.findHeaders { it.isNote() }.forEach { header ->
        parseElfNotes(reader, header).findNtGnuBuildId()?.let { return it.desc }
    }
    return null
}

@Throws(IOException::class)
private fun parseElfBuildIdNoteFromSectionHeader(reader: ByteReader, headers: ElfSectionHeaders): ByteArray? =
    headers.findHeader { it.isNote() && it.nameString == ELF_SECTION_NOTE_GNU_BUILD_ID }?.let {
        parseElfNotes(reader, it).findNtGnuBuildId()?.desc
    }

@Throws(IOException::class)
private fun hashElfTextSection(reader: ByteReader, headers: ElfSectionHeaders): ByteArray? =
    headers.findHeader { it.isProgbits() && it.nameString == ".text" }?.let {
        val buildId = ByteArray(GNU_BUILD_ID_SIZE)
        val oldOffset = reader.getCurrentOffset()
        val endOffset = it.offset + min(it.size, MAX_PAGE_SIZE)
        reader.seek(it.offset)
        while (reader.getCurrentOffset() < endOffset) {
            for (i in buildId.indices) {
                buildId[i] = buildId[i] xor reader.readByte()
            }
        }
        buildId.also { reader.seek(oldOffset) }
    }

private fun List<ElfNote>.findNtGnuBuildId(): ElfNote? = find { it.type == NT_GNU_BUILD_ID }
