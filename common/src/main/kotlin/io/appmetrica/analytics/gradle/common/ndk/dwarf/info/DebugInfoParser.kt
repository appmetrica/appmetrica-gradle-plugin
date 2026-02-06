package io.appmetrica.analytics.gradle.common.ndk.dwarf.info

import io.appmetrica.analytics.gradle.common.Log
import io.appmetrica.analytics.gradle.common.ndk.YSym
import io.appmetrica.analytics.gradle.common.ndk.dwarf.DwarfException
import io.appmetrica.analytics.gradle.common.ndk.dwarf.FileContext
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DWTag
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DebugAbbrevAttribute
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DebugAbbrevEntry
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.parseAbbrevSection
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.processor.CompileUnitAttributeProcessor
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.processor.InlineSubroutineAttributeProcessor
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.processor.SubProgramAttributeProcessor
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.reader.DebugAttributesReader
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.reader.SkipAttributesReader
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.range.NamedRangesResolver
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Throws(IOException::class, DwarfException::class)
fun parseDebugInfo(
    reader: ByteReader,
    fileContext: FileContext
): List<CompilationUnitContext> {
    val debugInfoHeader = fileContext.debugHeaders.debugInfo
    reader.seek(debugInfoHeader.offset)
    val sectionEndOffset = debugInfoHeader.offset + debugInfoHeader.size
    val compilationUnitContexts = mutableListOf<CompilationUnitContext>()
    while (reader.getCurrentOffset() != sectionEndOffset) {
        compilationUnitContexts.add(parseCompilationUnit(reader, fileContext))
    }
    return compilationUnitContexts
}

@Throws(IOException::class, DwarfException::class)
fun parseCompilationUnit(
    reader: ByteReader,
    fileContext: FileContext
): CompilationUnitContext {
    val header = reader.readCompilationUnitHeader(fileContext.debugHeaders.debugInfo.offset)
    val debugAbbrevOffset = fileContext.debugHeaders.debugAbbrev.offset + header.abbrevOffset
    return parseCompilationUnit(reader, fileContext, header, parseAbbrevSection(reader, debugAbbrevOffset))
}
/* ktlint-enable appmetrica-rules:no-top-level-members */

@Throws(IOException::class, DwarfException::class)
private fun parseCompilationUnit(
    reader: ByteReader,
    fileContext: FileContext,
    header: CompilationUnitHeader,
    abbrevEntries: Map<Int, DebugAbbrevEntry>
): CompilationUnitContext {
    val abbrevCode = reader.readULEB128()
    val abbrevEntry = abbrevEntries.getOrElse(abbrevCode) {
        throw DwarfException("Unrecognized abbreviations code: $abbrevCode")
    }
    val context = parseCompilationUnitEntry(reader, fileContext, header, abbrevEntry.attributes)
    if (abbrevEntry.hasChildren) {
        context.subPrograms.addAll(parseChildEntries(reader, context, abbrevEntries))
    }
    return context
}

@Throws(IOException::class, DwarfException::class)
private fun parseChildEntries(
    reader: ByteReader,
    context: CompilationUnitContext,
    abbrevEntries: Map<Int, DebugAbbrevEntry>,
    depth: Int = 0,
    subProgram: YSym.SubProgram? = null
): List<YSym.SubProgram> {
    val subPrograms = mutableListOf<YSym.SubProgram>()
    val debugInfoOffset = context.fileContext.debugHeaders.debugInfo.offset
    var abbrevCode: Int
    var entryOffset: Long
    while (
        run {
            entryOffset = reader.getCurrentOffset() - debugInfoOffset
            abbrevCode = reader.readULEB128()
            abbrevCode > 0
        }
    ) {
        val abbrevEntry = abbrevEntries.getOrElse(abbrevCode) {
            throw DwarfException("Unrecognized abbreviations code: $abbrevCode")
        }
        when (abbrevEntry.tag) {
            DWTag.SUBPROGRAM -> {
                val curSubProgram = parseSubProgram(reader, context, entryOffset, abbrevEntry.attributes)
                subPrograms.add(curSubProgram)
                if (abbrevEntry.hasChildren) {
                    subPrograms.addAll(parseChildEntries(reader, context, abbrevEntries, 1, curSubProgram))
                }
            }
            DWTag.INLINED_SUBROUTINE -> {
                val inline = parseInline(reader, context, entryOffset, abbrevEntry.attributes, depth)
                subProgram?.addInline(inline)
                if (abbrevEntry.hasChildren) {
                    subPrograms.addAll(parseChildEntries(reader, context, abbrevEntries, depth + 1, subProgram))
                }
            }
            else -> {
                skip(reader, context, abbrevEntry.attributes)
                if (abbrevEntry.hasChildren) {
                    subPrograms.addAll(parseChildEntries(reader, context, abbrevEntries, depth, subProgram))
                }
            }
        }
    }
    return subPrograms
}

@Throws(IOException::class)
private fun parseSubProgram(
    reader: ByteReader,
    context: CompilationUnitContext,
    entryOffset: Long,
    attributes: List<DebugAbbrevAttribute>
): YSym.SubProgram {
    val namedRangesResolver = NamedRangesResolver(
        reader,
        context.header.addressSize,
        context.fileContext.debugHeaders.debugRanges!!.offset
    )
    val attributeProcessor = SubProgramAttributeProcessor(entryOffset, context, namedRangesResolver)
    val attributesReader = DebugAttributesReader(
        reader,
        context.header,
        context.fileContext.referenceBytesConverter,
        attributeProcessor,
        context.fileContext.debugHeaders.debugStr.offset
    )
    return attributesReader.readAttributes(attributes)
}

private fun parseInline(
    reader: ByteReader,
    context: CompilationUnitContext,
    entryOffset: Long,
    attributes: List<DebugAbbrevAttribute>,
    depth: Int
): YSym.Inline {
    val namedRangesResolver = NamedRangesResolver(
        reader,
        context.header.addressSize,
        context.fileContext.debugHeaders.debugRanges!!.offset
    )
    val attributeProcessor = InlineSubroutineAttributeProcessor(entryOffset, context, namedRangesResolver, depth)
    val attributesReader = DebugAttributesReader(
        reader,
        context.header,
        context.fileContext.referenceBytesConverter,
        attributeProcessor,
        context.fileContext.debugHeaders.debugStr.offset
    )
    return attributesReader.readAttributes(attributes)
}

private fun skip(
    reader: ByteReader,
    context: CompilationUnitContext,
    attributes: List<DebugAbbrevAttribute>
) {
    SkipAttributesReader(reader, context.header).readAttributes(attributes)
}

@Throws(IOException::class)
private fun parseCompilationUnitEntry(
    reader: ByteReader,
    fileContext: FileContext,
    header: CompilationUnitHeader,
    attributes: List<DebugAbbrevAttribute>
): CompilationUnitContext {
    val attributeProcessor = CompileUnitAttributeProcessor(fileContext.referenceBytesConverter)
    val attributesReader = DebugAttributesReader(
        reader,
        header,
        fileContext.referenceBytesConverter,
        attributeProcessor,
        fileContext.debugHeaders.debugStr.offset
    )
    val entryData = attributesReader.readAttributes(attributes)
    val ranges = entryData.rangesSecOffset?.let {
        resolveRanges(reader, header.addressSize, fileContext.debugHeaders.debugRanges!!.offset, it, entryData.lowPc)
    } ?: listOf(entryData.lowPc to entryData.highPc)
    return CompilationUnitContext(fileContext, header, entryData, ranges)
}

private fun resolveRanges(
    reader: ByteReader,
    addressSize: Int,
    rangesSectionOffset: Long,
    offset: Long?,
    baseAddress: Long
): List<Pair<Long, Long>> {
    if (offset == null) {
        return emptyList()
    }
    val originalOffset = reader.getCurrentOffset()
    val ranges = mutableListOf<Pair<Long, Long>>()
    try {
        reader.seek(rangesSectionOffset + offset)
        var currentBaseAddress = baseAddress
        while (true) {
            var beginAddress = reader.readLong(addressSize)
            var endAddress = reader.readLong(addressSize)
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
    } catch (e: IOException) {
        Log.debug("Could not properly resolve range entries $e")
    } finally {
        reader.seek(originalOffset)
    }

    return ranges
}
