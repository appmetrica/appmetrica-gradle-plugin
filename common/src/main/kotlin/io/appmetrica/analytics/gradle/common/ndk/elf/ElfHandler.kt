package io.appmetrica.analytics.gradle.common.ndk.elf

import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.CompilationUnitContext
import io.appmetrica.analytics.gradle.common.ndk.dwarf.line.DebugLineData

interface ElfHandler {

    fun processElfHeader(fileHeader: ElfFileHeader)

    fun processSectionHeaders(sectionHeaders: ElfSectionHeaders)

    fun processElfBuildId(byteArray: ByteArray?)

    fun processArmVersion(armVersion: String)

    fun startProcessingSymbols()

    fun processElfSymbols(elfSymbols: List<ElfSymbol>)

    fun processDebugInfoCompilationUnit(debugInfoCompilationUnits: Map<CompilationUnitContext, DebugLineData>)

    fun endProcessingSymbols()
}
