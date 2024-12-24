package io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.reader

import io.appmetrica.analytics.gradle.common.ndk.dwarf.DWARF_VERSION_3
import io.appmetrica.analytics.gradle.common.ndk.dwarf.ReferenceBytesConverter
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DWForm
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DebugAbbrevAttribute
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.CompilationUnitHeader
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.processor.AttributeProcessor
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

class DebugAttributesReader<T>(
    private val reader: ByteReader,
    private val cuHeader: CompilationUnitHeader,
    private val referenceBytesConverter: ReferenceBytesConverter,
    private val attributeProcessor: AttributeProcessor<T>,
    private val debugStrOffset: Long
) : AttributesReader<T> {

    @Throws(IOException::class)
    override fun readAttributes(attributes: List<DebugAbbrevAttribute>): T {
        attributes.forEach { processDebugInfoEntryAttribute(it) }
        return attributeProcessor.finishProcessingAttributes()
    }

    @SuppressWarnings("ComplexMethod", "LongMethod")
    @Throws(IOException::class)
    private fun processDebugInfoEntryAttribute(attribute: DebugAbbrevAttribute) {
        when (attribute.form) {
            DWForm.ADDR -> {
                attributeProcessor.processAttribute(attribute.name, reader.readLong(cuHeader.addressSize))
            }
            DWForm.BLOCK -> {
                attributeProcessor.processAttribute(attribute.name, attribute.form, reader.readBytesWithBlockSize())
            }
            DWForm.BLOCK1 -> {
                val bytes = reader.readBytesWithBlockSize(Byte.SIZE_BYTES)
                attributeProcessor.processAttribute(attribute.name, attribute.form, bytes)
            }
            DWForm.BLOCK2 -> {
                val bytes = reader.readBytesWithBlockSize(Short.SIZE_BYTES)
                attributeProcessor.processAttribute(attribute.name, attribute.form, bytes)
            }
            DWForm.BLOCK4 -> {
                val bytes = reader.readBytesWithBlockSize(Int.SIZE_BYTES)
                attributeProcessor.processAttribute(attribute.name, attribute.form, bytes)
            }
            DWForm.DATA1 -> {
                attributeProcessor.processAttribute(attribute.name, attribute.form, reader.readBytes(Byte.SIZE_BYTES))
            }
            DWForm.DATA2 -> {
                attributeProcessor.processAttribute(attribute.name, attribute.form, reader.readBytes(Short.SIZE_BYTES))
            }
            DWForm.DATA4 -> {
                attributeProcessor.processAttribute(attribute.name, attribute.form, reader.readBytes(Int.SIZE_BYTES))
            }
            DWForm.DATA8 -> {
                attributeProcessor.processAttribute(attribute.name, attribute.form, reader.readBytes(Long.SIZE_BYTES))
            }
            DWForm.SDATA -> {
                attributeProcessor.processAttribute(attribute.name, reader.readSLEB128().toLong())
            }
            DWForm.UDATA -> {
                attributeProcessor.processAttribute(attribute.name, reader.readULEB128().toLong())
            }
            DWForm.STRING -> {
                attributeProcessor.processAttribute(attribute.name, reader.readNullTerminatedString(Charsets.UTF_8))
            }
            DWForm.FLAG -> {
                attributeProcessor.processAttribute(attribute.name, reader.readLong(Byte.SIZE_BYTES))
            }
            DWForm.FLAG_PRESENT -> {
                attributeProcessor.processAttribute(attribute.name, 1L)
            }
            DWForm.STRP -> {
                val value = reader.readStringFromTable(cuHeader.wordSize, debugStrOffset)
                attributeProcessor.processAttribute(attribute.name, value)
            }
            DWForm.REF1 -> {
                val value = referenceBytesConverter.asLongValue(reader.readBytes(Byte.SIZE_BYTES)) + cuHeader.offset
                attributeProcessor.processAttribute(attribute.name, value)
            }
            DWForm.REF2 -> {
                val value = referenceBytesConverter.asLongValue(reader.readBytes(Short.SIZE_BYTES)) + cuHeader.offset
                attributeProcessor.processAttribute(attribute.name, value)
            }
            DWForm.REF4 -> {
                val value = referenceBytesConverter.asLongValue(reader.readBytes(Int.SIZE_BYTES)) + cuHeader.offset
                attributeProcessor.processAttribute(attribute.name, value)
            }
            DWForm.REF8 -> {
                val value = referenceBytesConverter.asLongValue(reader.readBytes(Long.SIZE_BYTES)) + cuHeader.offset
                attributeProcessor.processAttribute(attribute.name, value)
            }
            DWForm.REF_ADDR -> {
                attributeProcessor.processAttribute(attribute.name, reader.readLong(cuHeader.getBytesCountForRefAddr()))
            }
            DWForm.REF_UDATA -> {
                attributeProcessor.processAttribute(attribute.name, reader.readULEB128().toLong() + cuHeader.offset)
            }
            DWForm.REF_SIG8 -> {
                attributeProcessor.processAttribute(attribute.name, attribute.form, reader.readBytes(Long.SIZE_BYTES))
            }
            DWForm.EXPRLOC -> {
                attributeProcessor.processAttribute(attribute.name, attribute.form, reader.readBytesWithBlockSize())
            }
            DWForm.SEC_OFFSET -> {
                attributeProcessor.processAttribute(attribute.name, reader.readLong(cuHeader.wordSize))
            }
            else -> {
                attributeProcessor.processAttribute(attribute.name, 0L)
            }
        }
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
