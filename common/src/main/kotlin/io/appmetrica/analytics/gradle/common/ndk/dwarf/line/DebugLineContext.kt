package io.appmetrica.analytics.gradle.common.ndk.dwarf.line

import io.appmetrica.analytics.gradle.common.ndk.dwarf.DWARF_VERSION_5
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DWForm
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.readIndexedFormIndex
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.CompilationUnitContext
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

private const val DW_LNCT_PATH = 1
private const val DW_LNCT_DIRECTORY_INDEX = 2
private const val DW_LNCT_TIMESTAMP = 3
private const val DW_LNCT_SIZE = 4
private const val DATA16_SIZE = 16
private const val SIZE_3 = 3

class DebugLineContext(val header: DebugLineHeader, val registers: DebugLineRegisters, val offsetSize: Int) {

    val directories = mutableListOf<String>()
    val files = mutableListOf<DebugLineFileInfo>()

    init {
        if (header.version < DWARF_VERSION_5) {
            directories.add("")
            files.add(DebugLineFileInfo("", "", 0, 0))
        }
    }

    fun defineDirectory(directory: String) {
        directories.add(directory)
    }

    fun defineFile(fileName: String, directoryIndex: Int, modTime: Int, length: Int) {
        val directory = directories.getOrElse(directoryIndex) { "" }
        files.add(DebugLineFileInfo(fileName, directory, modTime, length))
    }

    fun getFileInfo(fileIndex: Int) = files[fileIndex]

    class DebugLineFileInfo(val name: String, val directory: String, val modificationTime: Int, val length: Int)
}

private class LineEntryFormat(
    val contentType: Int,
    val form: DWForm,
    val implicitConst: Long
)

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Throws(IOException::class)
fun ByteReader.readDebugLineContext(
    offsetSize: Int,
    compilationUnit: CompilationUnitContext
): DebugLineContext {
    val header = readDebugLineHeader()
    val defaultFile = if (header.version >= DWARF_VERSION_5) 0 else 1
    val registers = DebugLineRegisters(header.defaultIsStatement, defaultFile)
    val context = DebugLineContext(header, registers, offsetSize)
    if (header.version >= DWARF_VERSION_5) {
        readDirectoriesV5(context, compilationUnit)
        readFilesV5(context, compilationUnit)
    } else {
        readDirectories(context)
        readFiles(context)
    }
    return context
}
/* ktlint-enable appmetrica-rules:no-top-level-members */

@Throws(IOException::class)
private fun ByteReader.readDirectories(context: DebugLineContext) {
    var directory: String
    while (readNullTerminatedString(Charsets.UTF_8).also { directory = it }.isNotEmpty()) {
        context.defineDirectory(directory)
    }
}

@Throws(IOException::class)
private fun ByteReader.readFiles(context: DebugLineContext) {
    var fileName: String
    while (readNullTerminatedString(Charsets.UTF_8).also { fileName = it }.isNotEmpty()) {
        context.defineFile(
            fileName = fileName,
            directoryIndex = readULEB128(),
            modTime = readULEB128(),
            length = readULEB128()
        )
    }
}

@Throws(IOException::class)
private fun ByteReader.readDirectoriesV5(
    context: DebugLineContext,
    compilationUnit: CompilationUnitContext
) {
    val format = readLineEntryFormats()
    val count = readULEB128()
    repeat(count) {
        var directory = ""
        format.forEach { entry ->
            val value = readLineFormatValue(entry, context, compilationUnit)
            if (entry.contentType == DW_LNCT_PATH) {
                directory = value as? String ?: directory
            }
        }
        context.defineDirectory(directory)
    }
}

@Throws(IOException::class)
private fun ByteReader.readFilesV5(
    context: DebugLineContext,
    compilationUnit: CompilationUnitContext
) {
    val format = readLineEntryFormats()
    val count = readULEB128()
    repeat(count) {
        var fileName = ""
        var directoryIndex = 0
        var modTime = 0
        var length = 0
        format.forEach { entry ->
            val value = readLineFormatValue(entry, context, compilationUnit)
            when (entry.contentType) {
                DW_LNCT_PATH -> fileName = value as? String ?: fileName
                DW_LNCT_DIRECTORY_INDEX -> directoryIndex = (value as? Long)?.toInt() ?: directoryIndex
                DW_LNCT_TIMESTAMP -> modTime = (value as? Long)?.toInt() ?: modTime
                DW_LNCT_SIZE -> length = (value as? Long)?.toInt() ?: length
            }
        }
        context.defineFile(fileName, directoryIndex, modTime, length)
    }
}

@Throws(IOException::class)
private fun ByteReader.readLineEntryFormats(): List<LineEntryFormat> {
    val count = readInt(Byte.SIZE_BYTES)
    return List(count) {
        val contentType = readULEB128()
        val form = DWForm.fromValue(readULEB128())
        val implicitConst = if (form == DWForm.IMPLICIT_CONST) readSLEB128().toLong() else 0
        LineEntryFormat(contentType, form, implicitConst)
    }
}

@Suppress("ComplexMethod")
@Throws(IOException::class)
private fun ByteReader.readLineFormatValue(
    entry: LineEntryFormat,
    context: DebugLineContext,
    compilationUnit: CompilationUnitContext
): Any? {
    val debugHeaders = compilationUnit.fileContext.debugHeaders
    return when (entry.form) {
        DWForm.STRING -> readNullTerminatedString(Charsets.UTF_8)
        DWForm.LINE_STRP -> readOffsetString(context.header.wordSize, debugHeaders.debugLineStr?.offset)
        DWForm.STRP -> readOffsetString(context.header.wordSize, debugHeaders.debugStr.offset)
        DWForm.STRX, DWForm.STRX1, DWForm.STRX2, DWForm.STRX3, DWForm.STRX4 ->
            compilationUnit.indexedValues.resolveString(readIndexedFormIndex(entry.form))
        DWForm.UDATA -> readULEB128().toLong()
        DWForm.DATA1 -> readLong(Byte.SIZE_BYTES)
        DWForm.DATA2 -> readLong(Short.SIZE_BYTES)
        DWForm.DATA4 -> readLong(Int.SIZE_BYTES)
        DWForm.DATA8 -> readLong(Long.SIZE_BYTES)
        DWForm.DATA16 -> {
            readBytes(DATA16_SIZE)
            null
        }
        DWForm.IMPLICIT_CONST -> entry.implicitConst
        DWForm.FLAG_PRESENT -> 1L
        DWForm.BLOCK -> {
            readBytesWithBlockSize()
            null
        }
        else -> {
            skipLineForm(entry.form, context.header.wordSize)
            null
        }
    }
}

@Throws(IOException::class)
private fun ByteReader.readOffsetString(wordSize: Int, sectionOffset: Long?): String {
    val offset = readLong(wordSize)
    if (sectionOffset == null) {
        return ""
    }
    val current = getCurrentOffset()
    seek(sectionOffset + offset)
    val value = readNullTerminatedString(Charsets.UTF_8)
    seek(current)
    return value
}

@Throws(IOException::class)
private fun ByteReader.skipLineForm(form: DWForm, wordSize: Int) {
    when (form) {
        DWForm.UDATA, DWForm.STRX, DWForm.ADDRX -> readULEB128()
        DWForm.DATA1, DWForm.FLAG, DWForm.STRX1, DWForm.ADDRX1 -> readBytes(Byte.SIZE_BYTES)
        DWForm.DATA2, DWForm.STRX2, DWForm.ADDRX2 -> readBytes(Short.SIZE_BYTES)
        DWForm.STRX3, DWForm.ADDRX3 -> readBytes(SIZE_3)
        DWForm.DATA4, DWForm.STRX4, DWForm.ADDRX4 -> readBytes(Int.SIZE_BYTES)
        DWForm.STRP, DWForm.LINE_STRP, DWForm.SEC_OFFSET -> readBytes(wordSize)
        DWForm.DATA8 -> readBytes(Long.SIZE_BYTES)
        DWForm.DATA16 -> readBytes(DATA16_SIZE)
        DWForm.STRING -> readNullTerminatedString(Charsets.UTF_8)
        DWForm.BLOCK -> readBytesWithBlockSize()
        else -> {}
    }
}
