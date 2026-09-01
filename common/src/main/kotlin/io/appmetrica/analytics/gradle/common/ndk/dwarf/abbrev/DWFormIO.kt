package io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev

import io.appmetrica.analytics.gradle.common.ndk.dwarf.DWARF_VERSION_3
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.CompilationUnitHeader
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

private const val DATA16_SIZE = 16
private const val SIZE_3 = 3

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Suppress("ComplexMethod")
@Throws(IOException::class)
fun ByteReader.skipDebugForm(form: DWForm, cuHeader: CompilationUnitHeader) {
    when (form) {
        DWForm.ADDR -> readLong(cuHeader.addressSize)
        DWForm.FLAG, DWForm.DATA1, DWForm.REF1, DWForm.STRX1, DWForm.ADDRX1 -> readBytes(Byte.SIZE_BYTES)
        DWForm.REF2, DWForm.DATA2, DWForm.STRX2, DWForm.ADDRX2 -> readBytes(Short.SIZE_BYTES)
        DWForm.STRX3, DWForm.ADDRX3 -> readBytes(SIZE_3)
        DWForm.REF4, DWForm.DATA4, DWForm.STRX4, DWForm.ADDRX4, DWForm.REF_SUP4 -> readBytes(Int.SIZE_BYTES)
        DWForm.REF8, DWForm.DATA8, DWForm.REF_SIG8, DWForm.REF_SUP8 -> readBytes(Long.SIZE_BYTES)
        DWForm.DATA16 -> readBytes(DATA16_SIZE)
        DWForm.UDATA, DWForm.REF_UDATA, DWForm.STRX, DWForm.ADDRX, DWForm.RNGLISTX, DWForm.LOCLISTX -> readULEB128()
        DWForm.REF_ADDR -> readBytes(cuHeader.getBytesCountForRefAddr())
        DWForm.SEC_OFFSET, DWForm.STRP, DWForm.LINE_STRP, DWForm.STRP_SUP -> readBytes(cuHeader.wordSize)
        DWForm.BLOCK1 -> readBytesWithBlockSize(Byte.SIZE_BYTES)
        DWForm.BLOCK2 -> readBytesWithBlockSize(Short.SIZE_BYTES)
        DWForm.BLOCK4 -> readBytesWithBlockSize(Int.SIZE_BYTES)
        DWForm.BLOCK, DWForm.EXPRLOC -> readBytesWithBlockSize()
        DWForm.SDATA -> readSLEB128()
        DWForm.STRING -> readNullTerminatedString(Charsets.UTF_8)
        DWForm.INDIRECT -> skipDebugForm(DWForm.fromValue(readULEB128()), cuHeader)
        DWForm.FLAG_PRESENT, DWForm.IMPLICIT_CONST, DWForm.UNKNOWN -> {}
    }
}

@Throws(IOException::class)
fun ByteReader.readIndexedFormIndex(form: DWForm): Long = when (form) {
    DWForm.STRX, DWForm.ADDRX, DWForm.RNGLISTX, DWForm.LOCLISTX -> readULEB128().toLong()
    DWForm.STRX1, DWForm.ADDRX1 -> readLong(Byte.SIZE_BYTES)
    DWForm.STRX2, DWForm.ADDRX2 -> readLong(Short.SIZE_BYTES)
    DWForm.STRX3, DWForm.ADDRX3 -> readLong(SIZE_3)
    DWForm.STRX4, DWForm.ADDRX4 -> readLong(Int.SIZE_BYTES)
    else -> throw IllegalArgumentException("Form $form is not an indexed DWARF form")
}

fun DWForm.isIndexedString() = this == DWForm.STRX || this == DWForm.STRX1 ||
    this == DWForm.STRX2 || this == DWForm.STRX3 || this == DWForm.STRX4

fun DWForm.isIndexedAddress() = this == DWForm.ADDRX || this == DWForm.ADDRX1 ||
    this == DWForm.ADDRX2 || this == DWForm.ADDRX3 || this == DWForm.ADDRX4

fun DWForm.isAddressClass() = this == DWForm.ADDR || isIndexedAddress()
/* ktlint-enable appmetrica-rules:no-top-level-members */

private fun CompilationUnitHeader.getBytesCountForRefAddr() =
    if (version < DWARF_VERSION_3) addressSize else wordSize
