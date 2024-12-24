package io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.processor

import io.appmetrica.analytics.gradle.common.ndk.dwarf.ReferenceBytesConverter
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DWAttribute
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DWForm
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.CompilationUnitContext

class CompileUnitAttributeProcessor(
    private val referenceBytesConverter: ReferenceBytesConverter
) : AttributeProcessor<CompilationUnitContext.EntryData> {

    private var name: String = ""
    private var lowPc: Long = 0
    private var highPc: Long = -1
    private var isHighPcAddress: Boolean = false
    private var stmtList: Long = 0
    private var rangesSecOffset: Long? = null

    override fun processAttribute(attribute: DWAttribute, form: DWForm, value: ByteArray) {
        when (attribute) {
            DWAttribute.STMT_LIST -> stmtList = referenceBytesConverter.asLongValue(value)
            DWAttribute.HIGH_PC -> {
                highPc = referenceBytesConverter.asLongValue(value)
                isHighPcAddress = false
            }
            else -> {
            }
        }
    }

    override fun processAttribute(attribute: DWAttribute, value: Long) {
        when (attribute) {
            DWAttribute.LOW_PC -> lowPc = value
            DWAttribute.STMT_LIST -> stmtList = value
            DWAttribute.RANGES -> rangesSecOffset = value
            DWAttribute.HIGH_PC -> {
                highPc = value
                isHighPcAddress = true
            }
            else -> {
            }
        }
    }

    override fun processAttribute(attribute: DWAttribute, value: String) {
        when (attribute) {
            DWAttribute.NAME -> name = value
            else -> {
            }
        }
    }

    override fun finishProcessingAttributes(): CompilationUnitContext.EntryData {
        if (isHighPcAddress == false) {
            highPc += lowPc
        }
        return CompilationUnitContext.EntryData(name, rangesSecOffset, lowPc, highPc, stmtList)
    }
}
