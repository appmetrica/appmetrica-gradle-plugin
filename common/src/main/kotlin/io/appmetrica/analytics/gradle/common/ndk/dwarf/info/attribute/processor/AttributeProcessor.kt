package io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.processor

import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DWAttribute
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DWForm

interface AttributeProcessor<T> {

    fun processAttribute(attribute: DWAttribute, form: DWForm, value: ByteArray)

    fun processAttribute(attribute: DWAttribute, value: Long)

    fun processAttribute(attribute: DWAttribute, value: String)

    fun finishProcessingAttributes(): T
}
