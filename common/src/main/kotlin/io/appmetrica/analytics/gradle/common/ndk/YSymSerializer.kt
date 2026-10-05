@file:Suppress("TooManyFunctions")

package io.appmetrica.analytics.gradle.common.ndk

import io.appmetrica.analytics.gradle.common.ndk.elf.ElfSymbol

object YSymSerializer {

    fun toString(sym: YSym): String {
        return StringBuilder()
            .appendLine("Object file")
            .appendlnHeader(sym)
            .appendlnDwarf(sym.compileUnits)
            .appendlnSymbols(sym.elfSymbols)
//            export
//            function starts
            .append("Object file end")
            .toString()
    }
}

private fun StringBuilder.appendlnHeader(sym: YSym) = apply {
    sym.run {
        appendLine("UUID:$identifier")
        appendLine("Architecture:$architecture")
//        Magic
//        Cputype
//        Cpusubtype
//        Filetype
//        Slide
    }
}

private fun StringBuilder.appendlnDwarf(compileUnits: List<YSym.CompileUnit>) = apply {
    appendLine("DWARF")
    compileUnits.forEach { appendlnCompileUnit(it) }
    appendLine("DWARF end")
}

private fun StringBuilder.appendlnCompileUnit(compileUnit: YSym.CompileUnit) = apply {
    compileUnit.run {
        appendLine("Compile unit:$name")
        appendlnRanges(ranges)
        appendlnFiles(files)
        appendlnLines(lines)
        appendlnSubPrograms(subPrograms)
        appendLine("Compile unit end")
    }
}

private fun StringBuilder.appendlnFiles(files: List<String>) = apply {
    appendLine("File names")
    files.forEachIndexed { index, file ->
        if (index != 0) {
            appendlnFile(index, file)
        }
    }
    appendLine("File names end")
}

private fun StringBuilder.appendlnFile(index: Int, file: String) = apply {
    appendLine("$index,$file")
}

private fun StringBuilder.appendlnLines(lines: List<YSym.Line>) = apply {
    appendLine("Line table")
    lines.forEach { appendlnLine(it) }
    appendLine("Line table end")
}

private fun StringBuilder.appendlnLine(line: YSym.Line) = apply {
    line.run {
        appendLine("${address.toHexString()},$file,$lineNumber,$column,${if (endSequence) "1" else "0"}")
    }
}

private fun StringBuilder.appendlnSubPrograms(subPrograms: List<YSym.SubProgram>) = apply {
    appendLine("Functions")
    subPrograms.forEach {
        if (it.ranges.isNotEmpty()) {
            appendlnSubProgram(it)
        }
    }
    appendLine("Functions end")
}

private fun StringBuilder.appendlnSubProgram(subProgram: YSym.SubProgram) = apply {
    subProgram.run {
        appendLine("Subprogram:${name.getSymbolName() ?: ""}")
        appendlnRanges(ranges)
        appendlnInlines(inlines)
    }
}

private fun StringBuilder.appendlnInlines(inlines: List<YSym.Inline>) = apply {
    inlines.forEach { appendlnInline(it) }
}

private fun StringBuilder.appendlnInline(inline: YSym.Inline) = apply {
    inline.run {
        appendLine("Inline:${name.getSymbolName() ?: ""}")
        appendLine("Depth:$depth")
        appendlnCaller(caller)
        appendlnRanges(ranges)
    }
}

private fun StringBuilder.appendlnCaller(caller: YSym.Inline.Caller) = apply {
    caller.run {
        appendLine("Caller:$file,$line,$column")
    }
}

private fun StringBuilder.appendlnRanges(ranges: List<Pair<Long, Long>>) = apply {
    appendLine("Ranges:${ranges.size}")
    ranges.forEach { appendlnRange(it) }
}

private fun StringBuilder.appendlnRange(range: Pair<Long, Long>) = apply {
    range.run {
        appendLine("${first.toHexString()},${second.toHexString()}")
    }
}

private fun StringBuilder.appendlnSymbols(symbols: List<ElfSymbol>) = apply {
    appendLine("Symbol table")
    symbols.forEach { appendlnSymbol(it) }
    appendLine("Symbol table end")
}

private fun StringBuilder.appendlnSymbol(symbol: ElfSymbol) = apply {
    symbol.run {
        if (isUndef() == false && isFunctionEntry()) {
            appendLine("${fixedValue.toHexString()},${size.toHexString()},F,$nameString")
        }
    }
}

private fun Long.toHexString() = java.lang.Long.toHexString(this)
