package io.appmetrica.analytics.gradle.common.ndk.elf

import io.appmetrica.analytics.gradle.common.ndk.dwarf.DwarfException
import io.appmetrica.analytics.gradle.common.ndk.dwarf.parseDwarf
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import io.appmetrica.analytics.gradle.common.ndk.io.RandomAccessFileInputStream
import io.appmetrica.analytics.gradle.common.utils.Log
import java.io.File
import java.io.IOException

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Throws(IOException::class, ElfException::class, DwarfException::class)
fun parseElf(input: File, handler: ElfHandler) {
    ByteReader(RandomAccessFileInputStream(input)).use { reader ->
        parseElf(reader, handler)
    }
}
/* ktlint-enable appmetrica-rules:no-top-level-members */

@Throws(IOException::class, ElfException::class, DwarfException::class)
private fun parseElf(reader: ByteReader, handler: ElfHandler) {
    reader.seek(0)
    Log.debug("Reading elf file ident")
    val elfFileIdent = reader.readElfFileIdent()
    Log.debug("Setting byte order ${elfFileIdent.elfData.byteOrder}")
    reader.setByteOrder(elfFileIdent.elfData.byteOrder)
    parseElf(reader, elfFileIdent, handler)
}

@Throws(IOException::class, ElfException::class, DwarfException::class)
private fun parseElf(reader: ByteReader, ident: ElfFileIdent, handler: ElfHandler) {
    Log.debug("Reading elf file header")
    val header = reader.readElfFileHeader(ident)
    handler.processElfHeader(header)

    Log.debug("Reading elf section headers")
    val sectionHeaders = parseElfSectionHeaders(reader, header)
    handler.processSectionHeaders(sectionHeaders)

    Log.debug("Reading elf build id")
    val programHeaders = parseElfProgramHeaders(reader, header)
    parseElfBuildId(reader, sectionHeaders, programHeaders).let {
        handler.processElfBuildId(it)
    }

    Log.debug("Reading arm version")
    parseArmVersion(reader, header, sectionHeaders)?.let { armVersion ->
        handler.processArmVersion(armVersion)
    } ?: Log.debug("Arm version not found")

    parseElfSymbols(reader, header, sectionHeaders, handler)
}

@Throws(IOException::class, DwarfException::class)
private fun parseElfSymbols(
    reader: ByteReader,
    elfFileHeader: ElfFileHeader,
    sectionHeaders: ElfSectionHeaders,
    handler: ElfHandler
) {
    handler.startProcessingSymbols()
    Log.debug("Reading elf symbols")
    handler.processElfSymbols(parseElfSymbols(reader, elfFileHeader, sectionHeaders))

    Log.debug("Reading debug elf section headers")
    DebugElfSectionHeaders.from(sectionHeaders)?.let { debugHeaders ->
        parseDwarf(reader, handler, elfFileHeader.ident.elfData.byteOrder, debugHeaders)
    } ?: Log.debug("Debug elf section headers not found")
    handler.endProcessingSymbols()
}
