package io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.processor

import io.appmetrica.analytics.gradle.common.ndk.YSym
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.CompilationUnitContext
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.range.NamedRangesResolver

class SubProgramAttributeProcessor(
    offset: Long,
    private val cuContext: CompilationUnitContext,
    private val namedRangesResolver: NamedRangesResolver
) : FuncAttributeProcessor<YSym.SubProgram>(offset, cuContext) {

    override fun finishProcessingAttributes(): YSym.SubProgram {
        val resolvedName = linkageName ?: name
        val nameProvider = getSymbolNameProvider(resolvedName)
        if (lowPc >= 0 && highPc >= 0) {
            if (isHighPcAddress == false) {
                highPc += lowPc
            }
            return YSym.SubProgram(nameProvider, listOf(lowPc to highPc))
        }
        return if (rangesOffset >= 0) {
            YSym.SubProgram(
                nameProvider,
                namedRangesResolver.resolveNamedRanges(rangesOffset, nameProvider, cuContext.lowPc)
                    .map { it.start to it.end }
            )
        } else {
            YSym.SubProgram(nameProvider, emptyList())
        }
    }
}
