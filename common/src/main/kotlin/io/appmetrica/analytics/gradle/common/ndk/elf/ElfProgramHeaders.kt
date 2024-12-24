package io.appmetrica.analytics.gradle.common.ndk.elf

import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

class ElfProgramHeaders(private val list: List<ElfProgramHeader>) {

    fun findHeaders(predicate: (ElfProgramHeader) -> Boolean) = list.filter(predicate)
}

@Throws(IOException::class)
fun parseElfProgramHeaders(reader: ByteReader, elfFileHeader: ElfFileHeader): ElfProgramHeaders {
    reader.seek(elfFileHeader.programHeaderOffset)
    val programHeaders = List(elfFileHeader.programHeaderNum) {
        reader.readElfProgramHeader(elfFileHeader.ident.elfClass)
    }
    return ElfProgramHeaders(programHeaders)
}
