package io.appmetrica.analytics.gradle.common.ndk.dwarf

import io.appmetrica.analytics.gradle.common.Log
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.CompilationUnitContext
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.parseDebugInfo
import io.appmetrica.analytics.gradle.common.ndk.dwarf.line.DebugLineData
import io.appmetrica.analytics.gradle.common.ndk.dwarf.line.parseDebugLinesFromCurrentOffset
import io.appmetrica.analytics.gradle.common.ndk.elf.DebugElfSectionHeaders
import io.appmetrica.analytics.gradle.common.ndk.elf.ElfHandler
import io.appmetrica.analytics.gradle.common.ndk.elf.ElfSectionHeader
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException
import java.nio.ByteOrder

@Throws(IOException::class, DwarfException::class)
fun parseDwarf(
    reader: ByteReader,
    handler: ElfHandler,
    byteOrder: ByteOrder,
    debugHeaders: DebugElfSectionHeaders
) {
    Log.debug("Processing debug headers : ${debugHeaders.getHeaderNames()}")

    val fileContext = FileContext(debugHeaders, ReferenceBytesConverter(byteOrder))

    Log.debug("Reading compilation unit contexts")
    handler.processDebugInfoCompilationUnit(
        parseDebugInfo(reader, fileContext).map { cuContext ->
            Log.debug("Reading debug line data for compilation unit ${cuContext.name}")
            cuContext to parseDebugLineData(reader, cuContext, debugHeaders.debugLine)
        }.toMap()
    )
}

@Throws(IOException::class)
fun parseDebugLineData(
    reader: ByteReader,
    context: CompilationUnitContext,
    debugLineSectionHeader: ElfSectionHeader
): DebugLineData {
    val debugLineOffset = context.debugLineOffset
    return if (debugLineOffset != null) {
        try {
            reader.seek(debugLineSectionHeader.offset + debugLineOffset)
            parseDebugLinesFromCurrentOffset(reader, context.header.addressSize)
        } catch (e: DwarfException) {
            Log.debug("Could not parse debug line data : $e")
            DebugLineData()
        }
    } else {
        Log.debug("Could not parse debug line data : debug line offset is null")
        DebugLineData()
    }
}
