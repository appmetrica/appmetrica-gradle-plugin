package io.appmetrica.analytics.gradle.common.ndk.dwarf.line

import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

class DebugLineContext(val header: DebugLineHeader, val registers: DebugLineRegisters, val offsetSize: Int) {

    val directories = mutableListOf("")
    val files = mutableListOf(DebugLineFileInfo("", "", 0, 0))

    fun defineDirectory(directory: String) {
        directories.add(directory)
    }

    fun defineFile(fileName: String, directoryIndex: Int, modTime: Int, length: Int) {
        files.add(DebugLineFileInfo(fileName, directories[directoryIndex], modTime, length))
    }

    fun getFileInfo(fileIndex: Int) = files[fileIndex]

    class DebugLineFileInfo(val name: String, val directory: String, val modificationTime: Int, val length: Int)
}

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Throws(IOException::class)
fun ByteReader.readDebugLineContext(offsetSize: Int): DebugLineContext {
    val header = readDebugLineHeader()
    val registers = DebugLineRegisters(header.defaultIsStatement)
    val context = DebugLineContext(header, registers, offsetSize)
    readDirectories(context)
    readFiles(context)
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
