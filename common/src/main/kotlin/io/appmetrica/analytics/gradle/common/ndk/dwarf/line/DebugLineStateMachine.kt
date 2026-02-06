package io.appmetrica.analytics.gradle.common.ndk.dwarf.line

import io.appmetrica.analytics.gradle.common.ndk.dwarf.DwarfException
import io.appmetrica.analytics.gradle.common.ndk.dwarf.line.opcode.processOpcode
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Throws(IOException::class, DwarfException::class)
fun parseDebugLinesFromCurrentOffset(
    reader: ByteReader,
    offsetSize: Int
): DebugLineData {
    val context = reader.readDebugLineContext(offsetSize)
    return DebugLineData(parseCompilationUnit(reader, context), context)
}
/* ktlint-enable appmetrica-rules:no-top-level-members */

@Throws(IOException::class, DwarfException::class)
private fun parseCompilationUnit(reader: ByteReader, context: DebugLineContext): List<DebugLineEntry> {
    val lineEntries = mutableListOf<DebugLineEntry>()
    while (reader.getCurrentOffset() < context.header.endOffset) {
        if (processOpcode(reader, context)) {
            lineEntries.add(context)
        }
        if (context.registers.isEndSequence) {
            lineEntries.add(context)
            context.registers.reset()
        }
    }
    return lineEntries
}

private fun MutableList<DebugLineEntry>.add(context: DebugLineContext) {
    add(
        DebugLineEntry(
            context.registers.address,
            context.registers.file,
            context.registers.line,
            context.registers.column,
            context.registers.isEndSequence
        )
    )
}
