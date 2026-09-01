package io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.reader

import io.appmetrica.analytics.gradle.common.ndk.dwarf.DWARF_VERSION_3
import io.appmetrica.analytics.gradle.common.ndk.dwarf.FileContext
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DWAttribute
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DWForm
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DebugAbbrevAttribute
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.isIndexedAddress
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.isIndexedString
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.readIndexedFormIndex
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.skipDebugForm
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.CompilationUnitHeader
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.DwarfIndexedValueResolver
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.processor.AttributeProcessor
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

private const val DATA16_SIZE = 16

class DebugAttributesReader<T>(
    private val reader: ByteReader,
    private val cuHeader: CompilationUnitHeader,
    private val fileContext: FileContext,
    private val attributeProcessor: AttributeProcessor<T>,
    private val indexedValues: DwarfIndexedValueResolver
) : AttributesReader<T> {

    private val debugHeaders = fileContext.debugHeaders
    private val referenceBytesConverter = fileContext.referenceBytesConverter

    @Throws(IOException::class)
    override fun readAttributes(attributes: List<DebugAbbrevAttribute>): T {
        val decoded = attributes.map { it to readRawAttribute(it) }
        decoded.forEach { (attribute, value) -> updateIndexedBases(attribute.name, value) }
        decoded.forEach { (attribute, value) -> processDecodedAttribute(attribute, value) }
        return attributeProcessor.finishProcessingAttributes()
    }

    @SuppressWarnings("ComplexMethod", "LongMethod")
    @Throws(IOException::class)
    private fun readRawAttribute(attribute: DebugAbbrevAttribute): RawAttributeValue {
        return readRawForm(attribute.form, attribute)
    }

    @SuppressWarnings("ComplexMethod", "LongMethod")
    @Throws(IOException::class)
    private fun readRawForm(form: DWForm, attribute: DebugAbbrevAttribute): RawAttributeValue {
        return when (form) {
            DWForm.ADDR -> RawAttributeValue.Number(form, reader.readLong(cuHeader.addressSize))
            DWForm.BLOCK -> RawAttributeValue.Bytes(form, reader.readBytesWithBlockSize())
            DWForm.BLOCK1 -> RawAttributeValue.Bytes(form, reader.readBytesWithBlockSize(Byte.SIZE_BYTES))
            DWForm.BLOCK2 -> RawAttributeValue.Bytes(form, reader.readBytesWithBlockSize(Short.SIZE_BYTES))
            DWForm.BLOCK4 -> RawAttributeValue.Bytes(form, reader.readBytesWithBlockSize(Int.SIZE_BYTES))
            DWForm.DATA1 -> RawAttributeValue.Bytes(form, reader.readBytes(Byte.SIZE_BYTES))
            DWForm.DATA2 -> RawAttributeValue.Bytes(form, reader.readBytes(Short.SIZE_BYTES))
            DWForm.DATA4 -> RawAttributeValue.Bytes(form, reader.readBytes(Int.SIZE_BYTES))
            DWForm.DATA8 -> RawAttributeValue.Bytes(form, reader.readBytes(Long.SIZE_BYTES))
            DWForm.DATA16 -> RawAttributeValue.Bytes(form, reader.readBytes(DATA16_SIZE))
            DWForm.SDATA -> RawAttributeValue.Number(form, reader.readSLEB128().toLong())
            DWForm.UDATA -> RawAttributeValue.Number(form, reader.readULEB128().toLong())
            DWForm.STRING -> RawAttributeValue.Text(reader.readNullTerminatedString(Charsets.UTF_8))
            DWForm.FLAG -> RawAttributeValue.Number(form, reader.readLong(Byte.SIZE_BYTES))
            DWForm.FLAG_PRESENT -> RawAttributeValue.Number(form, 1L)
            DWForm.STRP -> RawAttributeValue.Text(
                reader.readStringFromTable(cuHeader.wordSize, debugHeaders.debugStr.offset)
            )
            DWForm.LINE_STRP -> RawAttributeValue.Text(
                reader.readStringFromTable(
                    cuHeader.wordSize,
                    debugHeaders.debugLineStr?.offset ?: debugHeaders.debugStr.offset
                )
            )
            DWForm.REF1 -> RawAttributeValue.Number(
                form,
                referenceBytesConverter.asLongValue(reader.readBytes(Byte.SIZE_BYTES)) + cuHeader.offset
            )
            DWForm.REF2 -> RawAttributeValue.Number(
                form,
                referenceBytesConverter.asLongValue(reader.readBytes(Short.SIZE_BYTES)) + cuHeader.offset
            )
            DWForm.REF4 -> RawAttributeValue.Number(
                form,
                referenceBytesConverter.asLongValue(reader.readBytes(Int.SIZE_BYTES)) + cuHeader.offset
            )
            DWForm.REF8 -> RawAttributeValue.Number(
                form,
                referenceBytesConverter.asLongValue(reader.readBytes(Long.SIZE_BYTES)) + cuHeader.offset
            )
            DWForm.REF_ADDR -> RawAttributeValue.Number(form, reader.readLong(cuHeader.getBytesCountForRefAddr()))
            DWForm.REF_UDATA -> RawAttributeValue.Number(form, reader.readULEB128().toLong() + cuHeader.offset)
            DWForm.REF_SIG8 -> RawAttributeValue.Bytes(form, reader.readBytes(Long.SIZE_BYTES))
            DWForm.EXPRLOC -> RawAttributeValue.Bytes(form, reader.readBytesWithBlockSize())
            DWForm.SEC_OFFSET -> RawAttributeValue.Number(form, reader.readLong(cuHeader.wordSize))
            DWForm.IMPLICIT_CONST -> RawAttributeValue.Number(form, attribute.implicitConst)
            DWForm.INDIRECT -> readRawForm(DWForm.fromValue(reader.readULEB128()), attribute)
            else -> when {
                form.isIndexedString() -> RawAttributeValue.StringIndex(reader.readIndexedFormIndex(form))
                form.isIndexedAddress() -> RawAttributeValue.AddrIndex(reader.readIndexedFormIndex(form))
                form == DWForm.RNGLISTX -> RawAttributeValue.RnglistIndex(reader.readIndexedFormIndex(form))
                else -> {
                    reader.skipDebugForm(form, cuHeader)
                    RawAttributeValue.Number(form, 0L)
                }
            }
        }
    }

    private fun updateIndexedBases(attribute: DWAttribute, value: RawAttributeValue) {
        if (value !is RawAttributeValue.Number) {
            return
        }
        when (attribute) {
            DWAttribute.STR_OFFSETS_BASE -> indexedValues.strOffsetsBase = value.value
            DWAttribute.ADDR_BASE -> indexedValues.addrBase = value.value
            DWAttribute.RNGLISTS_BASE -> indexedValues.rnglistsBase = value.value
            else -> {}
        }
    }

    @Throws(IOException::class)
    private fun processDecodedAttribute(attribute: DebugAbbrevAttribute, value: RawAttributeValue) {
        when (value) {
            is RawAttributeValue.Number ->
                attributeProcessor.processAttribute(attribute.name, value.form, value.value)
            is RawAttributeValue.Text -> attributeProcessor.processAttribute(attribute.name, value.value)
            is RawAttributeValue.Bytes ->
                attributeProcessor.processAttribute(attribute.name, value.form, value.value)
            is RawAttributeValue.StringIndex ->
                attributeProcessor.processAttribute(attribute.name, indexedValues.resolveString(value.index))
            is RawAttributeValue.AddrIndex ->
                attributeProcessor.processAttribute(
                    attribute.name,
                    attribute.form,
                    indexedValues.resolveAddress(value.index)
                )
            is RawAttributeValue.RnglistIndex ->
                attributeProcessor.processAttribute(
                    attribute.name,
                    attribute.form,
                    indexedValues.resolveRnglistOffset(value.index)
                )
        }
    }

    private sealed class RawAttributeValue {
        class Number(val form: DWForm, val value: Long) : RawAttributeValue()
        class Text(val value: String) : RawAttributeValue()
        class Bytes(val form: DWForm, val value: ByteArray) : RawAttributeValue()
        class StringIndex(val index: Long) : RawAttributeValue()
        class AddrIndex(val index: Long) : RawAttributeValue()
        class RnglistIndex(val index: Long) : RawAttributeValue()
    }
}

@Throws(IOException::class)
private fun ByteReader.readStringFromTable(wordSize: Int, debugStrOffset: Long): String {
    val tableOffset = readLong(wordSize)
    val curOffset = getCurrentOffset()
    seek(debugStrOffset + tableOffset)
    val value = readNullTerminatedString(Charsets.UTF_8)
    seek(curOffset)
    return value
}

private fun CompilationUnitHeader.getBytesCountForRefAddr() = if (version < DWARF_VERSION_3) addressSize else wordSize
