package io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.reader

import io.appmetrica.analytics.gradle.common.ndk.dwarf.DWARF_VERSION_3
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DWForm
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DebugAbbrevAttribute
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.CompilationUnitHeader
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

class SkipAttributesReader(
    private val reader: ByteReader,
    private val cuHeader: CompilationUnitHeader
) : AttributesReader<Unit> {

    @Throws(IOException::class)
    override fun readAttributes(attributes: List<DebugAbbrevAttribute>) {
        attributes.forEach { skipDebugInfoEntryAttribute(it) }
    }

    @SuppressWarnings("ComplexMethod")
    @Throws(IOException::class)
    private fun skipDebugInfoEntryAttribute(attribute: DebugAbbrevAttribute) {
        when (attribute.form) {
            DWForm.ADDR -> reader.readLong(cuHeader.addressSize)
            DWForm.FLAG, DWForm.DATA1, DWForm.REF1 -> reader.readBytes(Byte.SIZE_BYTES)
            DWForm.REF2, DWForm.DATA2 -> reader.readBytes(Short.SIZE_BYTES)
            DWForm.REF4, DWForm.DATA4 -> reader.readBytes(Int.SIZE_BYTES)
            DWForm.REF8, DWForm.DATA8, DWForm.REF_SIG8 -> reader.readBytes(Long.SIZE_BYTES)
            DWForm.UDATA, DWForm.REF_UDATA -> reader.readULEB128()
            DWForm.REF_ADDR -> reader.readBytes(cuHeader.getBytesCountForRefAddr())
            DWForm.SEC_OFFSET, DWForm.STRP -> reader.readBytes(cuHeader.wordSize)
            DWForm.BLOCK1 -> reader.readBytesWithBlockSize(Byte.SIZE_BYTES)
            DWForm.BLOCK2 -> reader.readBytesWithBlockSize(Short.SIZE_BYTES)
            DWForm.BLOCK4 -> reader.readBytesWithBlockSize(Int.SIZE_BYTES)
            DWForm.BLOCK, DWForm.EXPRLOC -> reader.readBytesWithBlockSize()
            DWForm.SDATA -> reader.readSLEB128()
            DWForm.STRING -> reader.readNullTerminatedString(Charsets.UTF_8)
            else -> {}
        }
    }
}

private fun CompilationUnitHeader.getBytesCountForRefAddr() = if (version < DWARF_VERSION_3) addressSize else wordSize
