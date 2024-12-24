package io.appmetrica.analytics.gradle.common.ndk

import io.appmetrica.analytics.gradle.common.ndk.dwarf.SymbolNameProvider
import io.appmetrica.analytics.gradle.common.ndk.elf.ElfSymbol

class YSym private constructor(builder: Builder) {

    val identifier: String
    val elfSymbols: List<ElfSymbol>
    val compileUnits: List<CompileUnit>

    val codeId: String
    val architecture: String

    data class CompileUnit(
        val name: String,
        val ranges: List<Pair<Long, Long>>,
        val files: List<String>,
        val lines: List<Line>,
        val subPrograms: List<SubProgram>
    )

    class Line(
        val address: Long,
        val file: Int,
        val lineNumber: Long,
        val column: Long,
        val endSequence: Boolean
    )

    class SubProgram(
        val name: SymbolNameProvider,
        val ranges: List<Pair<Long, Long>>
    ) {
        private val _inlines = mutableListOf<Inline>()
        val inlines
            get() = _inlines.toList()

        fun addInline(inline: Inline) = apply {
            _inlines.add(inline)
        }
    }

    class Inline(
        val name: SymbolNameProvider,
        val depth: Int,
        val caller: Caller,
        val ranges: List<Pair<Long, Long>>
    ) {

        class Caller(
            val file: Long,
            val line: Long,
            val column: Long
        )
    }

    init {
        identifier = builder.identifier
        codeId = builder.codeId
        architecture = builder.architecture
        elfSymbols = builder.elfSymbols
        compileUnits = builder.compileUnits
    }

    class Builder(
        val identifier: String,
        val codeId: String,
        val architecture: String
    ) {

        private val _elfSymbols = mutableListOf<ElfSymbol>()
        val elfSymbols
            get() = _elfSymbols.toList()

        private val _compileUnits = mutableListOf<CompileUnit>()
        val compileUnits
            get() = _compileUnits.toList()

        fun addSymbol(symbol: ElfSymbol) = apply {
            _elfSymbols.add(symbol)
        }

        fun addCompileUnit(unit: CompileUnit) = apply {
            _compileUnits.add(unit)
        }

        fun build() = YSym(this)
    }
}
