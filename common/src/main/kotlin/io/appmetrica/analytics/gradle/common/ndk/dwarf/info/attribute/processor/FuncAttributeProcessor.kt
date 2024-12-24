package io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.processor

import io.appmetrica.analytics.gradle.common.ndk.dwarf.SimpleSymbolNameProvider
import io.appmetrica.analytics.gradle.common.ndk.dwarf.SymbolNameProvider
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DWAttribute
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DWForm
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.CompilationUnitContext

abstract class FuncAttributeProcessor<T>(
    private val offset: Long,
    private val cuContext: CompilationUnitContext
) : AttributeProcessor<T> {

    protected var name: String? = null
    protected var linkageName: String? = null
    protected var specification: Long = -1
    protected var abstractOrigin: Long = -1
    protected var highPc: Long = -1
    protected var isHighPcAddress: Boolean = false
    protected var lowPc: Long = -1
    protected var rangesOffset: Long = -1

    override fun processAttribute(attribute: DWAttribute, form: DWForm, value: ByteArray) {
        when (attribute) {
            DWAttribute.HIGH_PC -> {
                highPc = cuContext.fileContext.referenceBytesConverter.asLongValue(value)
                isHighPcAddress = false
            }
            else -> {
            }
        }
    }

    override fun processAttribute(attribute: DWAttribute, value: Long) {
        when (attribute) {
            DWAttribute.SPECIFICATION -> specification = value
            DWAttribute.ABSTRACT_ORIGIN -> abstractOrigin = value
            DWAttribute.LOW_PC -> lowPc = value
            DWAttribute.HIGH_PC -> {
                highPc = value
                isHighPcAddress = true
            }
            DWAttribute.RANGES -> rangesOffset = value
            else -> {
            }
        }
    }

    override fun processAttribute(attribute: DWAttribute, value: String) {
        when (attribute) {
            DWAttribute.NAME -> name = value
            DWAttribute.LINKAGE_NAME -> linkageName = value
            else -> {
            }
        }
    }

    protected fun getSymbolNameProvider(resolvedName: String?): SymbolNameProvider = if (resolvedName != null) {
        SimpleSymbolNameProvider(resolvedName)
    } else {
        ComplexNameProvider(
            ReferenceNameProvider(cuContext.fileContext.namesMapByOffset, specification),
            ReferenceNameProvider(cuContext.fileContext.namesMapByOffset, abstractOrigin)
        )
    }.also { cuContext.fileContext.namesMapByOffset[offset] = it }

    protected class ReferenceNameProvider(
        namesMapByOffset: Map<Long, SymbolNameProvider>,
        offset: Long
    ) : SymbolNameProvider {

        private val name: String? by lazy { namesMapByOffset[offset]?.getSymbolName() }

        override fun getSymbolName(): String? = name
    }

    protected class ComplexNameProvider(vararg providers: SymbolNameProvider) : SymbolNameProvider {

        private val name: String? by lazy { providers.find { it.getSymbolName() != null }?.getSymbolName() }

        override fun getSymbolName(): String? = name
    }
}
