package io.appmetrica.analytics.gradle.common.ndk.elf

import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

@Suppress("LongParameterList")
class ElfFileHeader(
    val ident: ElfFileIdent,
    val type: Int,
    val machine: ElfMachine,
    val version: Long,
    val entry: Long,
    val programHeaderOffset: Long,
    val sectionHeaderOffset: Long,
    val flags: Long,
    val elfHeaderSize: Int,
    val programHeaderEntrySize: Int,
    val programHeaderNum: Int,
    val sectionHeaderEntrySize: Int,
    val sectionHeaderNum: Int,
    val sectionHeaderStringIndex: Int
)

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Throws(IOException::class, ElfException::class)
fun ByteReader.readElfFileHeader(elfFileIdent: ElfFileIdent) = ElfFileHeader(
    ident = elfFileIdent,
    type = readInt(Short.SIZE_BYTES),
    machine = ElfMachine.fromValue(readInt(Short.SIZE_BYTES)),
    version = readLong(Int.SIZE_BYTES),
    entry = readLong(elfFileIdent.elfClass.wordSize),
    programHeaderOffset = readLong(elfFileIdent.elfClass.wordSize),
    sectionHeaderOffset = readLong(elfFileIdent.elfClass.wordSize),
    flags = readLong(Int.SIZE_BYTES),
    elfHeaderSize = readInt(Short.SIZE_BYTES),
    programHeaderEntrySize = readInt(Short.SIZE_BYTES),
    programHeaderNum = readInt(Short.SIZE_BYTES),
    sectionHeaderEntrySize = readInt(Short.SIZE_BYTES),
    sectionHeaderNum = readInt(Short.SIZE_BYTES),
    sectionHeaderStringIndex = readInt(Short.SIZE_BYTES)
)
/* ktlint-enable appmetrica-rules:no-top-level-members */
