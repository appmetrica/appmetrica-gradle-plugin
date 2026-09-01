package io.appmetrica.analytics.gradle.common.ndk.dwarf.info.range

import io.appmetrica.analytics.gradle.common.ndk.dwarf.DWARF_VERSION_5
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import io.appmetrica.analytics.gradle.common.utils.Log
import java.io.IOException

@SuppressWarnings("MagicNumber")
private object RangeListEntry {
    const val END_OF_LIST = 0x00
    const val BASE_ADDRESSX = 0x01
    const val STARTX_ENDX = 0x02
    const val STARTX_LENGTH = 0x03
    const val OFFSET_PAIR = 0x04
    const val BASE_ADDRESS = 0x05
    const val START_END = 0x06
    const val START_LENGTH = 0x07
}

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Throws(IOException::class)
fun ByteReader.resolveRangeList(
    addressSize: Int,
    rangeListOffset: Long,
    baseAddress: Long,
    version: Int,
    resolveIndexedAddress: (Long) -> Long
): List<Pair<Long, Long>> {
    val originalOffset = getCurrentOffset()
    try {
        seek(rangeListOffset)
        return if (version >= DWARF_VERSION_5) {
            readDwarf5RangeList(addressSize, baseAddress, resolveIndexedAddress)
        } else {
            readDwarf4RangeList(addressSize, baseAddress)
        }
    } catch (e: IOException) {
        Log.debug("Could not properly resolve range entries $e")
    } finally {
        seek(originalOffset)
    }
    return emptyList()
}
/* ktlint-enable appmetrica-rules:no-top-level-members */

@Throws(IOException::class)
private fun ByteReader.readDwarf4RangeList(addressSize: Int, baseAddress: Long): List<Pair<Long, Long>> {
    val ranges = mutableListOf<Pair<Long, Long>>()
    var currentBaseAddress = baseAddress
    while (true) {
        var beginAddress = readLong(addressSize)
        var endAddress = readLong(addressSize)
        if (beginAddress == 0L && endAddress == 0L) {
            break
        }
        if (beginAddress == -1L) {
            currentBaseAddress = endAddress
        } else {
            beginAddress += currentBaseAddress
            endAddress += currentBaseAddress
            ranges.add(beginAddress to endAddress)
        }
    }
    return ranges
}

@Suppress("ComplexMethod", "LoopWithTooManyJumpStatements")
@Throws(IOException::class)
private fun ByteReader.readDwarf5RangeList(
    addressSize: Int,
    baseAddress: Long,
    resolveIndexedAddress: (Long) -> Long
): List<Pair<Long, Long>> {
    val ranges = mutableListOf<Pair<Long, Long>>()
    var currentBaseAddress = baseAddress
    while (true) {
        when (readInt(Byte.SIZE_BYTES)) {
            RangeListEntry.END_OF_LIST -> break
            RangeListEntry.BASE_ADDRESSX -> {
                currentBaseAddress = resolveIndexedAddress(readULEB128().toLong())
            }
            RangeListEntry.STARTX_ENDX -> {
                val start = resolveIndexedAddress(readULEB128().toLong())
                val end = resolveIndexedAddress(readULEB128().toLong())
                ranges.add(start to end)
            }
            RangeListEntry.STARTX_LENGTH -> {
                val start = resolveIndexedAddress(readULEB128().toLong())
                ranges.add(start to start + readULEB128().toLong())
            }
            RangeListEntry.OFFSET_PAIR -> {
                val start = currentBaseAddress + readULEB128().toLong()
                val end = currentBaseAddress + readULEB128().toLong()
                ranges.add(start to end)
            }
            RangeListEntry.BASE_ADDRESS -> {
                currentBaseAddress = readLong(addressSize)
            }
            RangeListEntry.START_END -> {
                val start = readLong(addressSize)
                val end = readLong(addressSize)
                ranges.add(start to end)
            }
            RangeListEntry.START_LENGTH -> {
                val start = readLong(addressSize)
                ranges.add(start to start + readULEB128().toLong())
            }
            else -> return ranges
        }
    }
    return ranges
}
