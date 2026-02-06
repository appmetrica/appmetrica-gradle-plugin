package io.appmetrica.analytics.gradle.common.ndk.dwarf.info

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
    val endOffset: Long
)

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Throws(IOException::class)
fun ByteReader.readCompilationUnitHeader(debugInfoOffset: Long): CompilationUnitHeader {
    val offset = getCurrentOffset() - debugInfoOffset
    val (wordSize, length) = readCompilationUnitLength()
    val endOffset = getCurrentOffset() + length
    return CompilationUnitHeader(
        offset = offset,
        length = length,
        version = readInt(Short.SIZE_BYTES),
        abbrevOffset = readLong(wordSize),
        addressSize = readInt(Byte.SIZE_BYTES),
        wordSize = wordSize,
        endOffset = endOffset
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
