package io.appmetrica.analytics.gradle.common.ndk.elf

import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

/* ktlint-disable appmetrica-rules:no-top-level-members */
const val ELF_SECTION_DEBUG_INFO = ".debug_info"
const val ELF_SECTION_DEBUG_ABBREV = ".debug_abbrev"
const val ELF_SECTION_DEBUG_STR = ".debug_str"
const val ELF_SECTION_DEBUG_RANGES = ".debug_ranges"
const val ELF_SECTION_DEBUG_LINE = ".debug_line"
const val ELF_SECTION_NOTE_GNU_BUILD_ID = ".note.gnu.build-id"

class ElfSectionHeaders(private val list: List<ElfSectionHeader>) {

    val headersByName = list.map { it.nameString to it }.toMap()

    fun getHeaderByName(name: String) = headersByName[name]

    fun getHeaderByIndex(index: Int) = list.getOrNull(index)

    fun findHeader(predicate: (ElfSectionHeader) -> Boolean) = list.find(predicate)

    fun findHeaders(predicate: (ElfSectionHeader) -> Boolean) = list.filter(predicate)

    fun hasDebugInfo() = getHeaderByName(ELF_SECTION_DEBUG_INFO) != null
}

@Throws(IOException::class)
fun parseElfSectionHeaders(reader: ByteReader, elfFileHeader: ElfFileHeader): ElfSectionHeaders {
    reader.seek(elfFileHeader.sectionHeaderOffset)
    val sectionHeaders = List(elfFileHeader.sectionHeaderNum) {
        reader.readElfSectionHeader(elfFileHeader.ident.elfClass)
    }
    val namesSection = sectionHeaders[elfFileHeader.sectionHeaderStringIndex]
    sectionHeaders.readNames(reader, namesSection.offset)
    return ElfSectionHeaders(sectionHeaders)
}
/* ktlint-enable appmetrica-rules:no-top-level-members */
