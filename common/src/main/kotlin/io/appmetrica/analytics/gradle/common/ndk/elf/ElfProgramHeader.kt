package io.appmetrica.analytics.gradle.common.ndk.elf

import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

private const val PT_NULL = 0x0
private const val PT_LOAD = 0x1
private const val PT_DYNAMIC = 0x2
private const val PT_INTERP = 0x3
private const val PT_NOTE = 0x4
private const val PT_SHLIB = 0x5
private const val PT_PHDR = 0x6
private const val PT_TLS = 0x7
private const val PT_NUM = 0x8
private const val PT_LOOS = 0x60000000
private const val PT_GNU_EH_FRAME = 0x6474e550
private const val PT_GNU_STACK = 0x6474e551
private const val PT_GNU_RELRO = 0x6474e552
private const val PT_LOSUNW = 0x6ffffffa
private const val PT_SUNWBSS = 0x6ffffffa
private const val PT_SUNWSTACK = 0x6ffffffb
private const val PT_HISUNW = 0x6fffffff
private const val PT_HIOS = 0x6fffffff
private const val PT_LOPROC = 0x70000000
private const val PT_HIPROC = 0x7fffffff

class ElfProgramHeader(
    val type: Int,
    val flags: Int,
    val offset: Long,
    val virtualAddress: Long,
    val physicalAddress: Long,
    val fileSize: Long,
    val memorySize: Long,
    val align: Long
) {

    fun isNote() = type == PT_NOTE
}

@Throws(IOException::class)
fun ByteReader.readElfProgramHeader(elfClass: ElfClass): ElfProgramHeader = when (elfClass) {
    ElfClass.ELF_CLASS_32 -> {
        val type = readInt()
        val offset = readLong(elfClass.wordSize)
        val virtualAddress = readLong(elfClass.wordSize)
        val physicalAddress = readLong(elfClass.wordSize)
        val fileSize = readLong(elfClass.wordSize)
        val memorySize = readLong(elfClass.wordSize)
        val flags = readInt()
        val align = readLong(elfClass.wordSize)
        ElfProgramHeader(type, flags, offset, virtualAddress, physicalAddress, fileSize, memorySize, align)
    }
    ElfClass.ELF_CLASS_64 -> {
        val type = readInt()
        val flags = readInt()
        val offset = readLong(elfClass.wordSize)
        val virtualAddress = readLong(elfClass.wordSize)
        val physicalAddress = readLong(elfClass.wordSize)
        val fileSize = readLong(elfClass.wordSize)
        val memorySize = readLong(elfClass.wordSize)
        val align = readLong(elfClass.wordSize)
        ElfProgramHeader(type, flags, offset, virtualAddress, physicalAddress, fileSize, memorySize, align)
    }
}
