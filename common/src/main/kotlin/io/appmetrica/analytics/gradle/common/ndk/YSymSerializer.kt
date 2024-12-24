@file:SuppressWarnings("TooManyFunctions")

package io.appmetrica.analytics.gradle.common.ndk

import io.appmetrica.analytics.gradle.common.ndk.elf.ElfSymbol

object YSymSerializer {

    fun toString(sym: YSym): String {
        return StringBuilder()
            .appendln("Object file")
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
        appendln("UUID:$identifier")
        appendln("Architecture:$architecture")
//        Magic
//        Cputype
//        Cpusubtype
//        Filetype
//        Slide
    }
}

private fun StringBuilder.appendlnDwarf(compileUnits: List<YSym.CompileUnit>) = apply {
    appendln("DWARF")
    compileUnits.forEach { appendlnCompileUnit(it) }
    appendln("DWARF end")
}

private fun StringBuilder.appendlnCompileUnit(compileUnit: YSym.CompileUnit) = apply {
    compileUnit.run {
        appendln("Compile unit:$name")
        appendlnRanges(ranges)
        appendlnFiles(files)
        appendlnLines(lines)
        appendlnSubPrograms(subPrograms)
        appendln("Compile unit end")
    }
}

private fun StringBuilder.appendlnFiles(files: List<String>) = apply {
    appendln("File names")
    files.forEachIndexed { index, file ->
        if (index != 0) {
            appendlnFile(index, file)
        }
    }
    appendln("File names end")
}

private fun StringBuilder.appendlnFile(index: Int, file: String) = apply {
    appendln("$index,$file")
}

private fun StringBuilder.appendlnLines(lines: List<YSym.Line>) = apply {
    appendln("Line table")
    lines.forEach { appendlnLine(it) }
    appendln("Line table end")
}

private fun StringBuilder.appendlnLine(line: YSym.Line) = apply {
    line.run {
        appendln("${address.toHexString()},$file,$lineNumber,$column,${if (endSequence) "1" else "0"}")
    }
}

private fun StringBuilder.appendlnSubPrograms(subPrograms: List<YSym.SubProgram>) = apply {
    appendln("Functions")
    subPrograms.forEach {
        if (it.ranges.isNotEmpty()) {
            appendlnSubProgram(it)
        }
    }
    appendln("Functions end")
}

private fun StringBuilder.appendlnSubProgram(subProgram: YSym.SubProgram) = apply {
    subProgram.run {
        appendln("Subprogram:${name.getSymbolName() ?: ""}")
        appendlnRanges(ranges)
        appendlnInlines(inlines)
    }
}

private fun StringBuilder.appendlnInlines(inlines: List<YSym.Inline>) = apply {
    inlines.forEach { appendlnInline(it) }
}

private fun StringBuilder.appendlnInline(inline: YSym.Inline) = apply {
    inline.run {
        appendln("Inline:${name.getSymbolName() ?: ""}")
        appendln("Depth:$depth")
        appendlnCaller(caller)
        appendlnRanges(ranges)
    }
}

private fun StringBuilder.appendlnCaller(caller: YSym.Inline.Caller) = apply {
    caller.run {
        appendln("Caller:$file,$line,$column")
    }
}

private fun StringBuilder.appendlnRanges(ranges: List<Pair<Long, Long>>) = apply {
    appendln("Ranges:${ranges.size}")
    ranges.forEach { appendlnRange(it) }
}

private fun StringBuilder.appendlnRange(range: Pair<Long, Long>) = apply {
    range.run {
        appendln("${first.toHexString()},${second.toHexString()}")
    }
}

private fun StringBuilder.appendlnSymbols(symbols: List<ElfSymbol>) = apply {
    appendln("Symbol table")
    symbols.forEach { appendlnSymbol(it) }
    appendln("Symbol table end")
}

private fun StringBuilder.appendlnSymbol(symbol: ElfSymbol) = apply {
    symbol.run {
        if (isUndef() == false && isFunctionEntry()) {
            appendln("${fixedValue.toHexString()},${size.toHexString()},F,$nameString")
        }
    }
}

private fun Long.toHexString() = java.lang.Long.toHexString(this)
