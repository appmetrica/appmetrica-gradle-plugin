package io.appmetrica.analytics.gradle.common.ndk.dwarf.info

import io.appmetrica.analytics.gradle.common.ndk.YSym
import io.appmetrica.analytics.gradle.common.ndk.dwarf.FileContext
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.range.NamedRange

class CompilationUnitContext private constructor(
    val name: String,
    val ranges: List<Pair<Long, Long>>,
    val fileContext: FileContext,
    val header: CompilationUnitHeader,
    val lowPc: Long = 0,
    val highPc: Long = -1,
    val debugLineOffset: Long? = null
) {

    val namedRanges = mutableListOf<NamedRange>()

    val subPrograms = mutableListOf<YSym.SubProgram>()

    constructor(
        fileContext: FileContext,
        header: CompilationUnitHeader,
        entryData: EntryData,
        ranges: List<Pair<Long, Long>>
    ) : this(entryData.name, ranges, fileContext, header, entryData.lowPc, entryData.lowPc, entryData.stmtList)

    class EntryData(val name: String, val rangesSecOffset: Long?, val lowPc: Long, val highPc: Long, val stmtList: Long)
}
