package io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.processor

import io.appmetrica.analytics.gradle.common.ndk.YSym
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DWAttribute
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DWForm
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.CompilationUnitContext
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.range.NamedRangesResolver

class InlineSubroutineAttributeProcessor(
    offset: Long,
    private val cuContext: CompilationUnitContext,
    private val namedRangesResolver: NamedRangesResolver,
    private val depth: Int
) : FuncAttributeProcessor<YSym.Inline>(offset, cuContext) {

    private var callFile: Long = 0
    private var callLine: Long = 0
    private var callColumn: Long = 0

    override fun processAttribute(attribute: DWAttribute, form: DWForm, value: ByteArray) {
        when (attribute) {
            DWAttribute.CALL_FILE -> {
                callFile = cuContext.fileContext.referenceBytesConverter.asLongValue(value)
            }
            DWAttribute.CALL_LINE -> {
                callLine = cuContext.fileContext.referenceBytesConverter.asLongValue(value)
            }
            DWAttribute.CALL_COLUMN -> {
                callColumn = cuContext.fileContext.referenceBytesConverter.asLongValue(value)
            }
            else -> super.processAttribute(attribute, form, value)
        }
    }

    override fun finishProcessingAttributes(): YSym.Inline {
        val resolvedName = linkageName ?: name
        val nameProvider = getSymbolNameProvider(resolvedName)
        val caller = YSym.Inline.Caller(callFile, callLine, callColumn)
        if (lowPc >= 0 && highPc >= 0) {
            if (isHighPcAddress == false) {
                highPc += lowPc
            }
            return YSym.Inline(nameProvider, depth, caller, listOf(lowPc to highPc))
        }
        return if (rangesOffset >= 0) {
            YSym.Inline(
                nameProvider,
                depth,
                caller,
                namedRangesResolver.resolveNamedRanges(rangesOffset, nameProvider, cuContext.lowPc)
                    .map { it.start to it.end }
            )
        } else {
            YSym.Inline(nameProvider, depth, caller, emptyList())
        }
    }
}
