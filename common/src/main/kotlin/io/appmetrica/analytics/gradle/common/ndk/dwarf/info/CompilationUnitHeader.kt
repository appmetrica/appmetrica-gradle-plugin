package io.appmetrica.analytics.gradle.common.ndk.dwarf.info

import io.appmetrica.analytics.gradle.common.ndk.dwarf.DWARF_VERSION_5
import io.appmetrica.analytics.gradle.common.ndk.dwarf.DW_UT_SKELETON
import io.appmetrica.analytics.gradle.common.ndk.dwarf.DW_UT_SPLIT_COMPILE
import io.appmetrica.analytics.gradle.common.ndk.dwarf.DW_UT_SPLIT_TYPE
import io.appmetrica.analytics.gradle.common.ndk.dwarf.DW_UT_TYPE
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

private const val DWARF_32_WORD_SIZE = 4
private const val DWARF_64_WORD_SIZE = 8

@Suppress("LongParameterList")
class CompilationUnitHeader(
    val offset: Long,
    val length: Long,
    val version: Int,
    val abbrevOffset: Long,
    val addressSize: Int,
    val wordSize: Int,
    val endOffset: Long,
    val unitType: Int = 0
)

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Throws(IOException::class)
fun ByteReader.readCompilationUnitHeader(debugInfoOffset: Long): CompilationUnitHeader {
    val offset = getCurrentOffset() - debugInfoOffset
    val (wordSize, length) = readCompilationUnitLength()
    val endOffset = getCurrentOffset() + length
    val version = readInt(Short.SIZE_BYTES)
    val unitType: Int
    val addressSize: Int
    val abbrevOffset: Long
    if (version >= DWARF_VERSION_5) {
        unitType = readInt(Byte.SIZE_BYTES)
        addressSize = readInt(Byte.SIZE_BYTES)
        abbrevOffset = readLong(wordSize)
        skipDwarf5UnitTypeFields(unitType, wordSize)
    } else {
        unitType = 0
        abbrevOffset = readLong(wordSize)
        addressSize = readInt(Byte.SIZE_BYTES)
    }
    return CompilationUnitHeader(
        offset = offset,
        length = length,
        version = version,
        abbrevOffset = abbrevOffset,
        addressSize = addressSize,
        wordSize = wordSize,
        endOffset = endOffset,
        unitType = unitType
    )
}
/* ktlint-enable appmetrica-rules:no-top-level-members */

@Throws(IOException::class)
private fun ByteReader.readCompilationUnitLength(): Pair<Int, Long> {
    var wordSize = DWARF_32_WORD_SIZE
    var length = readLong(wordSize)
    if (length == -1L) {
        wordSize = DWARF_64_WORD_SIZE
        length = readLong(wordSize)
    }
    return wordSize to length
}

@Throws(IOException::class)
private fun ByteReader.skipDwarf5UnitTypeFields(unitType: Int, wordSize: Int) {
    when (unitType) {
        DW_UT_SKELETON, DW_UT_SPLIT_COMPILE -> readLong(Long.SIZE_BYTES)
        DW_UT_TYPE, DW_UT_SPLIT_TYPE -> {
            readLong(Long.SIZE_BYTES)
            readLong(wordSize)
        }
    }
}
