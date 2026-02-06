@file:Suppress("UnusedPrivateProperty")

package io.appmetrica.analytics.gradle.common.ndk.elf

import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

private const val SHT_NULL = 0x0
private const val SHT_PROGBITS = 0x1
private const val SHT_SYMTAB = 0x2
private const val SHT_STRTAB = 0x3
private const val SHT_RELA = 0x4
private const val SHT_HASH = 0x5
private const val SHT_DYNAMIC = 0x6
private const val SHT_NOTE = 0x7
private const val SHT_NOBITS = 0x8
private const val SHT_REL = 0x9
private const val SHT_SHLIB = 0xa
private const val SHT_DYNSYM = 0xb
private const val SHT_ARM_ATTRIBUTES = 0x70000003

@Suppress("LongParameterList")
class ElfSectionHeader(
    val name: Int,
    val type: Int,
    val flags: Long,
    val address: Long,
    val offset: Long,
    val size: Long,
    val link: Int,
    val info: Int,
    val addressAlign: Long,
    val entrySize: Long
) {

    lateinit var nameString: String

    fun isProgbits() = type == SHT_PROGBITS

    fun isSymTab() = type == SHT_SYMTAB

    fun isNote() = type == SHT_NOTE

    fun isArmAttributes() = type == SHT_ARM_ATTRIBUTES
}

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Throws(IOException::class)
fun ByteReader.readElfSectionHeader(elfClass: ElfClass) = ElfSectionHeader(
    name = readInt(),
    type = readInt(),
    flags = readLong(elfClass.wordSize),
    address = readLong(elfClass.wordSize),
    offset = readLong(elfClass.wordSize),
    size = readLong(elfClass.wordSize),
    link = readInt(),
    info = readInt(),
    addressAlign = readLong(elfClass.wordSize),
    entrySize = readLong(elfClass.wordSize)
)

@Throws(IOException::class)
fun List<ElfSectionHeader>.readNames(reader: ByteReader, namesSectionOffset: Long) {
    sortedBy { it.name }.forEach { sectionHeader ->
        reader.seek(namesSectionOffset + sectionHeader.name.toLong())
        sectionHeader.nameString = reader.readNullTerminatedString(Charsets.UTF_8)
    }
}
/* ktlint-enable appmetrica-rules:no-top-level-members */
