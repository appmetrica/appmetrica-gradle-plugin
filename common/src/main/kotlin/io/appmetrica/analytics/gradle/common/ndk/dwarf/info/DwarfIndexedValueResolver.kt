package io.appmetrica.analytics.gradle.common.ndk.dwarf.info

import io.appmetrica.analytics.gradle.common.ndk.dwarf.DwarfException
import io.appmetrica.analytics.gradle.common.ndk.elf.DebugElfSectionHeaders
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

class DwarfIndexedValueResolver(
    private val reader: ByteReader,
    private val header: CompilationUnitHeader,
    private val debugHeaders: DebugElfSectionHeaders
) {

    var strOffsetsBase: Long = 0
    var addrBase: Long = 0
    var rnglistsBase: Long = 0

    @Throws(IOException::class, DwarfException::class)
    fun resolveString(index: Long): String {
        val strOffsets = debugHeaders.debugStrOffsets
            ?: throw DwarfException("Missing .debug_str_offsets for indexed string form")
        val currentOffset = reader.getCurrentOffset()
        reader.seek(strOffsets.offset + strOffsetsBase + index * header.wordSize)
        val stringOffset = reader.readLong(header.wordSize)
        reader.seek(debugHeaders.debugStr.offset + stringOffset)
        val value = reader.readNullTerminatedString(Charsets.UTF_8)
        reader.seek(currentOffset)
        return value
    }

    @Throws(IOException::class, DwarfException::class)
    fun resolveAddress(index: Long): Long {
        val debugAddr = debugHeaders.debugAddr
            ?: throw DwarfException("Missing .debug_addr for indexed address form")
        val currentOffset = reader.getCurrentOffset()
        reader.seek(debugAddr.offset + addrBase + index * header.addressSize)
        val value = reader.readLong(header.addressSize)
        reader.seek(currentOffset)
        return value
    }

    @Throws(IOException::class, DwarfException::class)
    fun resolveRnglistOffset(index: Long): Long {
        val rnglists = debugHeaders.debugRnglists
            ?: throw DwarfException("Missing .debug_rnglists for indexed range list form")
        val currentOffset = reader.getCurrentOffset()
        reader.seek(rnglists.offset + rnglistsBase + index * header.wordSize)
        val offset = reader.readLong(header.wordSize)
        reader.seek(currentOffset)
        return rnglistsBase + offset
    }
}
