package io.appmetrica.analytics.gradle.common.ndk.dwarf.info

import io.appmetrica.analytics.gradle.common.ndk.YSym
import io.appmetrica.analytics.gradle.common.ndk.dwarf.DWARF_VERSION_5
import io.appmetrica.analytics.gradle.common.ndk.dwarf.DW_UT_COMPILE
import io.appmetrica.analytics.gradle.common.ndk.dwarf.DW_UT_PARTIAL
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
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.range.resolveRangeList
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
    while (reader.getCurrentOffset() < sectionEndOffset) {
        parseCompilationUnit(reader, fileContext)?.let { compilationUnitContexts.add(it) }
    }
    return compilationUnitContexts
}

@Throws(IOException::class, DwarfException::class)
fun parseCompilationUnit(
    reader: ByteReader,
    fileContext: FileContext
): CompilationUnitContext? {
    val header = reader.readCompilationUnitHeader(fileContext.debugHeaders.debugInfo.offset)
    try {
        if (header.version >= DWARF_VERSION_5 &&
            header.unitType != DW_UT_COMPILE &&
            header.unitType != DW_UT_PARTIAL
        ) {
            return null
        }
        val debugAbbrevOffset = fileContext.debugHeaders.debugAbbrev.offset + header.abbrevOffset
        val indexedValues = DwarfIndexedValueResolver(reader, header, fileContext.debugHeaders)
        return parseCompilationUnit(
            reader,
            fileContext,
            header,
            parseAbbrevSection(reader, debugAbbrevOffset),
            indexedValues
        )
    } finally {
        reader.seek(header.endOffset)
    }
}
/* ktlint-enable appmetrica-rules:no-top-level-members */

@Throws(IOException::class, DwarfException::class)
private fun parseCompilationUnit(
    reader: ByteReader,
    fileContext: FileContext,
    header: CompilationUnitHeader,
    abbrevEntries: Map<Int, DebugAbbrevEntry>,
    indexedValues: DwarfIndexedValueResolver
): CompilationUnitContext {
    val abbrevCode = reader.readULEB128()
    val abbrevEntry = abbrevEntries.getOrElse(abbrevCode) {
        throw DwarfException("Unrecognized abbreviations code: $abbrevCode")
    }
    val context = parseCompilationUnitEntry(reader, fileContext, header, abbrevEntry.attributes, indexedValues)
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
    val attributeProcessor = SubProgramAttributeProcessor(
        entryOffset,
        context,
        context.createNamedRangesResolver(reader)
    )
    val attributesReader = DebugAttributesReader(
        reader,
        context.header,
        context.fileContext,
        attributeProcessor,
        context.indexedValues
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
    val attributeProcessor = InlineSubroutineAttributeProcessor(
        entryOffset,
        context,
        context.createNamedRangesResolver(reader),
        depth
    )
    val attributesReader = DebugAttributesReader(
        reader,
        context.header,
        context.fileContext,
        attributeProcessor,
        context.indexedValues
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
    attributes: List<DebugAbbrevAttribute>,
    indexedValues: DwarfIndexedValueResolver
): CompilationUnitContext {
    val attributeProcessor = CompileUnitAttributeProcessor(fileContext.referenceBytesConverter)
    val attributesReader = DebugAttributesReader(
        reader,
        header,
        fileContext,
        attributeProcessor,
        indexedValues
    )
    val entryData = attributesReader.readAttributes(attributes)
    val ranges = entryData.rangesSecOffset?.let { offset ->
        header.rangesSectionOffset(fileContext)?.let { sectionOffset ->
            reader.resolveRangeList(
                header.addressSize,
                sectionOffset + offset,
                entryData.lowPc,
                header.version,
                indexedValues::resolveAddress
            )
        }
    } ?: listOf(entryData.lowPc to entryData.highPc)
    return CompilationUnitContext(fileContext, header, entryData, ranges, indexedValues)
}

private fun CompilationUnitHeader.rangesSectionOffset(fileContext: FileContext) =
    if (version >= DWARF_VERSION_5) {
        fileContext.debugHeaders.debugRnglists?.offset
    } else {
        fileContext.debugHeaders.debugRanges?.offset
    }

private fun CompilationUnitContext.createNamedRangesResolver(reader: ByteReader) = NamedRangesResolver(
    reader,
    header.addressSize,
    header.rangesSectionOffset(fileContext),
    header.version,
    indexedValues::resolveAddress
)
