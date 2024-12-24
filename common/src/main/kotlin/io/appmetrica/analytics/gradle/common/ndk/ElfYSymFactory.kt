package io.appmetrica.analytics.gradle.common.ndk

import io.appmetrica.analytics.gradle.common.Log
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.CompilationUnitContext
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.range.NamedRanges
import io.appmetrica.analytics.gradle.common.ndk.dwarf.line.DebugLineData
import io.appmetrica.analytics.gradle.common.ndk.elf.ElfFileHeader
import io.appmetrica.analytics.gradle.common.ndk.elf.ElfHandler
import io.appmetrica.analytics.gradle.common.ndk.elf.ElfMachine
import io.appmetrica.analytics.gradle.common.ndk.elf.ElfSectionHeaders
import io.appmetrica.analytics.gradle.common.ndk.elf.ElfSymbol
import io.appmetrica.analytics.gradle.common.ndk.elf.parseElf
import io.appmetrica.analytics.gradle.common.ndk.io.print
import io.appmetrica.analytics.gradle.common.ndk.io.toPrettyString
import java.io.File
import java.io.IOException
import java.nio.ByteOrder

private const val HEX = 16

class ElfYSymFactory : YSymFactory {

    @Throws(YSymException::class, IOException::class)
    override fun createCSymFromFile(binFile: File): YSym {
        Log.debug("Processing file ${binFile.absolutePath}")
        if (!binFile.isFile) {
            throw IllegalArgumentException("Invalid object file: $binFile")
        }
        val handler = YSymFactoryHandler()
        parseElf(binFile, handler)
        return handler.builder.build()
    }

    @SuppressWarnings("TooManyFunctions")
    private class YSymFactoryHandler : ElfHandler {

        lateinit var builder: YSym.Builder
            private set
        private lateinit var arch: ElfMachine
        private lateinit var archName: String
        private var isArm: Boolean = false
        private var hasDebugInfo: Boolean = false
        private lateinit var identifier: String
        private lateinit var codeId: String

        override fun processElfHeader(fileHeader: ElfFileHeader) {
            Log.debug("Processing elf file header : ${fileHeader.toPrettyString()}")
            arch = fileHeader.machine
            isArm = arch == ElfMachine.EM_ARM || arch == ElfMachine.EM_AARCH64
            archName = fileHeader.machine.archName
        }

        override fun processSectionHeaders(sectionHeaders: ElfSectionHeaders) {
            Log.debug("Processing section headers")
            sectionHeaders.print(Log::debug)
            hasDebugInfo = sectionHeaders.hasDebugInfo()
        }

        override fun processElfBuildId(byteArray: ByteArray?) {
            if (byteArray == null) {
                throw YSymException("Not found ELF build id") // TODO change text
            }
            Log.debug("Processing elf build id : ${byteArray.toPrettyString()}")

            identifier = byteArray.toUuidString() + "0"
            Log.debug("UUID: $identifier")

            codeId = byteArray.toHexString()
            Log.debug("Code id: $codeId")
        }

        override fun processArmVersion(armVersion: String) {
            Log.debug("Processing elf arm version : $armVersion")
            if (arch == ElfMachine.EM_ARM) {
                archName += "v$armVersion"
            }
        }

        override fun startProcessingSymbols() {
            val generationLog = if (hasDebugInfo) {
                "using ELF symbols and DWARF elfData for ySYM generation."
            } else {
                "using ELF elfData for ySYM generation."
            }
            Log.debug(
                """
                    =================================================================================
                    Start processing symbols $generationLog
                    =================================================================================
                """.trimIndent()
            )
            builder = YSym.Builder(identifier, codeId, archName)
        }

        override fun processElfSymbols(elfSymbols: List<ElfSymbol>) {
            Log.debug("Processing elf symbols")
            elfSymbols.print(Log::debug)
            elfSymbols.forEach {
                builder.addSymbol(it)
            }
        }

        override fun processDebugInfoCompilationUnit(
            debugInfoCompilationUnits: Map<CompilationUnitContext, DebugLineData>
        ) {
            Log.debug("Processing debug info compilation units")
            debugInfoCompilationUnits.forEach { (context, debugLineData) ->
                Log.debug("Processing debug info compilation unit : ${context.name}")
                Log.debug(
                    "Subprogram symbol names : " +
                        context.subPrograms.map { it.name.getSymbolName() }.toPrettyString()
                )

                val namedRanges = NamedRanges(context.namedRanges)
                val files = debugLineData.debugLineContext?.files?.map { "${it.directory}/${it.name}" } ?: emptyList()
                Log.debug("Debug line context file names : ${files.toPrettyString()}")

                val lines = debugLineData.debugLineEntries.map {
                    YSym.Line(it.address, it.file, it.lineNumber, it.columnNumber, it.endSequence)
                }
                builder.addCompileUnit(
                    YSym.CompileUnit(context.name, context.ranges, files, lines, context.subPrograms)
                )
            }
        }

        override fun endProcessingSymbols() {
            Log.debug(
                """
                    =================================================================================
                    End processing symbols
                    =================================================================================
                """.trimIndent()
            )
        }

        private fun ByteArray.toHexString() = joinToString("") { it.toHexString(2) }

        private fun Byte.toHexString(size: Int = 0) = toUByte().toString(HEX).padStart(size, '0').toUpperCase()

//      https://github.com/google/breakpad/blob/78f7ae495bc147e97a58e8158072fd35fdd99419/src/common/linux/file_id.cc#L178-L193
        @SuppressWarnings("MagicNumber")
        private fun ByteArray.toUuidString(): String {
            val bytes = this
            if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                return bytes.toHexString()
            } else {
                val extendedBytes = bytes.copyOf(16)

                val answer = mutableListOf<Byte>()
                answer += extendedBytes.slice(0 until 4).reversed()
                answer += extendedBytes.slice(4 until 6).reversed()
                answer += extendedBytes.slice(6 until 8).reversed()
                answer += extendedBytes.slice(8 until 16)

                return answer.toByteArray().toHexString()
            }
        }
    }
}
