package io.appmetrica.analytics.gradle.common.ndk.dwarf.line

import io.appmetrica.analytics.gradle.common.ndk.dwarf.DWARF_VERSION_4
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

private const val DWARF_32_WORD_SIZE = 4
private const val DWARF_64_WORD_SIZE = 8

class DebugLineHeader(
    val length: Long,
    val version: Int,
    val headerLength: Long,
    val minInstructionLength: Byte,
    val maxOperationsPerInstruction: Byte,
    val defaultIsStatement: Boolean,
    val lineBase: Byte,
    val lineRange: Byte,
    val opcodeBase: Byte,
    val standardOpcodeLengths: ByteArray,
    val endOffset: Long
)

@Throws(IOException::class)
fun ByteReader.readDebugLineHeader(): DebugLineHeader {
    val (wordSize, length) = readDebugLineLength()
    val endOffset = getCurrentOffset() + length
    val version = readInt(Short.SIZE_BYTES)
    val headerLength = readLong(wordSize)
    val minInstructionLength = readByte()
    val maxOperationsPerInstruction = if (version >= DWARF_VERSION_4) readByte() else 1
    val defaultIsStatement = readInt(Byte.SIZE_BYTES) != 0
    val lineBase = readByte()
    val lineRange = readByte()
    val opcodeBase = readByte()
    val standardOpcodeLengths = ByteArray(opcodeBase.toInt())
    for (i in 1 until opcodeBase) {
        standardOpcodeLengths[i] = readByte()
    }
    return DebugLineHeader(
        length,
        version,
        headerLength,
        minInstructionLength,
        maxOperationsPerInstruction,
        defaultIsStatement,
        lineBase,
        lineRange,
        opcodeBase,
        standardOpcodeLengths,
        endOffset
    )
}

@Throws(IOException::class)
fun ByteReader.readDebugLineLength(): Pair<Int, Long> {
    var wordSize = DWARF_32_WORD_SIZE
    var length = readLong(wordSize)
    if (length == -1L) {
        wordSize = DWARF_64_WORD_SIZE
        length = readLong(wordSize)
    }
    return wordSize to length
}
