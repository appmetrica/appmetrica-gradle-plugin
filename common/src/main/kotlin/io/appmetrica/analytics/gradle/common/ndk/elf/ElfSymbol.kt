@file:Suppress("UnusedPrivateProperty")

package io.appmetrica.analytics.gradle.common.ndk.elf

import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException
import kotlin.experimental.and

private const val STB_LOCAL = 0x0
private const val STB_GLOBAL = 0x1
private const val STB_WEAK = 0x2
private const val STB_LOOS = 0xa
private const val STB_HIOS = 0xc
private const val STB_LOPROC = 0xd
private const val STB_HIPROC = 0xf

private const val STT_NOTYPE = 0x0
private const val STT_OBJECT = 0x1
private const val STT_FUNC = 0x2
private const val STT_SECTION = 0x3
private const val STT_FILE = 0x4
private const val STT_LOOS = 0xa
private const val STT_HIOS = 0xc
private const val STT_LOPROC = 0xd
private const val STT_HIPROC = 0xf

private const val SHN_UNDEF = 0x0.toShort()
private const val SHN_ABS = 0xFFF1.toShort()

data class ElfSymbol(
    val name: Int,
    val value: Long,
    val size: Long,
    val info: Byte,
    val other: Byte,
    val sectionTableIndex: Short
) {

    lateinit var nameString: String
    var fixedValue: Long = -1
        private set

    fun isFunctionEntry() = (info and 0xF).toInt() == STT_FUNC

    fun isUndef() = sectionTableIndex == SHN_UNDEF

    fun isAbs() = sectionTableIndex == SHN_ABS

    fun fixValue(machine: ElfMachine) {
        fixedValue = if (isAbs()) {
            value
        } else if (machine in listOf(ElfMachine.EM_ARM, ElfMachine.EM_MIPS) && isFunctionEntry()) {
            value and 1L.inv()
        } else {
            value
        }
    }
}

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Throws(IOException::class)
fun parseElfSymbols(
    reader: ByteReader,
    elfFileHeader: ElfFileHeader,
    sectionHeaders: ElfSectionHeaders
): List<ElfSymbol> {
    val symbols = mutableListOf<ElfSymbol>()
    sectionHeaders.findHeaders { it.isSymTab() }.forEach { symTabSection ->
        sectionHeaders.getHeaderByIndex(symTabSection.link)?.let { namesSection ->
            symbols.addAll(parseElfSymbols(reader, elfFileHeader, symTabSection, namesSection))
        }
    }
    return symbols
}
/* ktlint-enable appmetrica-rules:no-top-level-members */

@Throws(IOException::class)
private fun parseElfSymbols(
    reader: ByteReader,
    elfFileHeader: ElfFileHeader,
    symTabSection: ElfSectionHeader,
    namesSection: ElfSectionHeader
): List<ElfSymbol> {
    reader.seek(symTabSection.offset)
    val numSymbols = (symTabSection.size / symTabSection.entrySize).toInt()
    val symbols = List(numSymbols) {
        reader.readElfSymbol(elfFileHeader.ident.elfClass).apply {
            fixValue(elfFileHeader.machine)
        }
    }
    symbols.readNames(reader, namesSection.offset)
    return symbols
}

@Throws(IOException::class)
private fun ByteReader.readElfSymbol(elfClass: ElfClass) = when (elfClass) {
    ElfClass.ELF_CLASS_32 -> {
        val name = readInt()
        val value = readLong(elfClass.wordSize)
        val size = readLong(elfClass.wordSize)
        val info = readByte()
        val other = readByte()
        val sectionTableIndex = readShort()
        ElfSymbol(name, value, size, info, other, sectionTableIndex)
    }
    ElfClass.ELF_CLASS_64 -> {
        val name = readInt()
        val info = readByte()
        val other = readByte()
        val sectionTableIndex = readShort()
        val value = readLong(elfClass.wordSize)
        val size = readLong(elfClass.wordSize)
        ElfSymbol(name, value, size, info, other, sectionTableIndex)
    }
}

@Throws(IOException::class)
private fun List<ElfSymbol>.readNames(reader: ByteReader, namesSectionOffset: Long) {
    sortedBy { it.name }.forEach { symbol ->
        reader.seek(namesSectionOffset + symbol.name)
        symbol.nameString = reader.readNullTerminatedString(Charsets.UTF_8)
    }
}
