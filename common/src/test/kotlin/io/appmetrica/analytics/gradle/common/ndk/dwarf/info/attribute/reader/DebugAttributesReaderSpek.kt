package io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.reader

import io.appmetrica.analytics.gradle.common.ndk.dwarf.DWARF_VERSION_5
import io.appmetrica.analytics.gradle.common.ndk.dwarf.FileContext
import io.appmetrica.analytics.gradle.common.ndk.dwarf.ReferenceBytesConverter
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DWAttribute
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DWForm
import io.appmetrica.analytics.gradle.common.ndk.dwarf.abbrev.DebugAbbrevAttribute
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.CompilationUnitHeader
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.DwarfIndexedValueResolver
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.attribute.processor.CompileUnitAttributeProcessor
import io.appmetrica.analytics.gradle.common.ndk.elf.DebugElfSectionHeaders
import io.appmetrica.analytics.gradle.common.ndk.elf.ElfSectionHeader
import io.appmetrica.analytics.gradle.common.ndk.io.ByteArraySeekableInputStream
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import org.assertj.core.api.SoftAssertions
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val ADDRESS_SIZE = 8
private const val WORD_SIZE = 4
private const val LOW_PC = 0x1000L
private const val HIGH_PC_OFFSET = 0x20L
private const val HIGH_PC_ADDRESS = 0x3000L

object DebugAttributesReaderSpek : Spek({
    describe("DW_AT_high_pc") {
        it("adds constant-class values to low_pc and keeps address-class values absolute") {
            SoftAssertions.assertSoftly { softly ->
                softly.assertThat(
                    readHighPc(DWForm.IMPLICIT_CONST, byteArrayOf(), implicitConst = HIGH_PC_OFFSET)
                ).isEqualTo(LOW_PC + HIGH_PC_OFFSET)
                softly.assertThat(readHighPc(DWForm.UDATA, byteArrayOf(HIGH_PC_OFFSET.toByte())))
                    .isEqualTo(LOW_PC + HIGH_PC_OFFSET)
                softly.assertThat(readHighPc(DWForm.DATA4, le32(HIGH_PC_OFFSET.toInt())))
                    .isEqualTo(LOW_PC + HIGH_PC_OFFSET)
                softly.assertThat(readHighPc(DWForm.ADDR, le64(HIGH_PC_ADDRESS)))
                    .isEqualTo(HIGH_PC_ADDRESS)
            }
        }
    }
})

private fun readHighPc(
    highPcForm: DWForm,
    highPcBytes: ByteArray,
    implicitConst: Long = 0
): Long {
    val bytes = le64(LOW_PC) + highPcBytes
    val reader = ByteReader(ByteArraySeekableInputStream(bytes)).apply {
        setByteOrder(ByteOrder.LITTLE_ENDIAN)
    }
    val debugHeaders = DebugElfSectionHeaders(
        debugInfo = dummySectionHeader(),
        debugAbbrev = dummySectionHeader(),
        debugStr = dummySectionHeader(),
        debugLine = dummySectionHeader(),
        debugRanges = null
    )
    val cuHeader = CompilationUnitHeader(
        offset = 0,
        length = bytes.size.toLong(),
        version = DWARF_VERSION_5,
        abbrevOffset = 0,
        addressSize = ADDRESS_SIZE,
        wordSize = WORD_SIZE,
        endOffset = bytes.size.toLong()
    )
    val fileContext = FileContext(debugHeaders, ReferenceBytesConverter(ByteOrder.LITTLE_ENDIAN))
    val processor = CompileUnitAttributeProcessor(fileContext.referenceBytesConverter)
    val attributesReader = DebugAttributesReader(
        reader,
        cuHeader,
        fileContext,
        processor,
        DwarfIndexedValueResolver(reader, cuHeader, debugHeaders)
    )
    return attributesReader.readAttributes(
        listOf(
            DebugAbbrevAttribute(DWAttribute.LOW_PC, DWForm.ADDR),
            DebugAbbrevAttribute(DWAttribute.HIGH_PC, highPcForm, implicitConst)
        )
    ).highPc
}

private fun dummySectionHeader() = ElfSectionHeader(
    name = 0,
    type = 1,
    flags = 0,
    address = 0,
    offset = 0,
    size = 0,
    link = 0,
    info = 0,
    addressAlign = 1,
    entrySize = 0
).apply { nameString = ".debug_dummy" }

private fun le64(value: Long): ByteArray =
    ByteBuffer.allocate(Long.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array()

private fun le32(value: Int): ByteArray =
    ByteBuffer.allocate(Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
