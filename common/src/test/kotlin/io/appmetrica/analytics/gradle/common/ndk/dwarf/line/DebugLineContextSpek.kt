package io.appmetrica.analytics.gradle.common.ndk.dwarf.line

import io.appmetrica.analytics.gradle.common.ndk.dwarf.DWARF_VERSION_5
import io.appmetrica.analytics.gradle.common.ndk.dwarf.FileContext
import io.appmetrica.analytics.gradle.common.ndk.dwarf.ReferenceBytesConverter
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.CompilationUnitContext
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.CompilationUnitHeader
import io.appmetrica.analytics.gradle.common.ndk.dwarf.info.DwarfIndexedValueResolver
import io.appmetrica.analytics.gradle.common.ndk.elf.DebugElfSectionHeaders
import io.appmetrica.analytics.gradle.common.ndk.elf.ElfSectionHeader
import io.appmetrica.analytics.gradle.common.ndk.io.ByteArraySeekableInputStream
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.ByteArrayOutputStream
import java.nio.ByteOrder

private const val ADDRESS_SIZE = 8
private const val WORD_SIZE = 4
private const val DW_LNCT_PATH = 1
private const val DW_LNCT_TIMESTAMP = 3
private const val DW_LNCT_SIZE = 4
private const val DW_FORM_STRING = 8
private const val DW_FORM_BLOCK = 9
private const val DW_FORM_UDATA = 15
private const val FIRST_FILE_SIZE = 42
private const val SECOND_FILE_SIZE = 7

object DebugLineContextSpek : Spek({
    describe("DWARF 5 line table file names") {
        it("consumes block timestamp and keeps the following fields aligned") {
            val bytes = dwarf5LineHeaderWithBlockTimestamp()
            val reader = ByteReader(ByteArraySeekableInputStream(bytes)).apply {
                setByteOrder(ByteOrder.LITTLE_ENDIAN)
            }
            val compilationUnit = dummyCompilationUnit(reader)

            val context = reader.readDebugLineContext(WORD_SIZE, compilationUnit)

            SoftAssertions.assertSoftly { softly ->
                softly.assertThat(context.files).hasSize(2)
                softly.assertThat(context.files[0].name).isEqualTo("main.c")
                softly.assertThat(context.files[0].directory).isEqualTo("src")
                softly.assertThat(context.files[0].length).isEqualTo(FIRST_FILE_SIZE)
                softly.assertThat(context.files[1].name).isEqualTo("other.c")
                softly.assertThat(context.files[1].directory).isEqualTo("src")
                softly.assertThat(context.files[1].length).isEqualTo(SECOND_FILE_SIZE)
            }
            assertThat(context.directories).containsExactly("src")
        }
    }
})

private fun dummyCompilationUnit(reader: ByteReader): CompilationUnitContext {
    val debugHeaders = DebugElfSectionHeaders(
        debugInfo = dummySectionHeader(),
        debugAbbrev = dummySectionHeader(),
        debugStr = dummySectionHeader(),
        debugLine = dummySectionHeader(),
        debugRanges = null
    )
    val cuHeader = CompilationUnitHeader(
        offset = 0,
        length = 0,
        version = DWARF_VERSION_5,
        abbrevOffset = 0,
        addressSize = ADDRESS_SIZE,
        wordSize = WORD_SIZE,
        endOffset = 0
    )
    val fileContext = FileContext(debugHeaders, ReferenceBytesConverter(ByteOrder.LITTLE_ENDIAN))
    return CompilationUnitContext(
        fileContext,
        cuHeader,
        CompilationUnitContext.EntryData("", null, 0, -1, 0),
        emptyList(),
        DwarfIndexedValueResolver(reader, cuHeader, debugHeaders)
    )
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

private fun dwarf5LineHeaderWithBlockTimestamp(): ByteArray {
    val unit = ByteArrayOutputStream()
    fun u8(value: Int) = unit.write(value and 0xFF)
    fun u16(value: Int) {
        u8(value)
        u8(value ushr 8)
    }
    fun u32(value: Int) {
        u8(value)
        u8(value ushr 8)
        u8(value ushr 16)
        u8(value ushr 24)
    }
    fun str(value: String) {
        unit.write(value.toByteArray(Charsets.UTF_8))
        u8(0)
    }

    u16(DWARF_VERSION_5)
    u8(ADDRESS_SIZE)
    u8(0)
    u32(0)
    u8(1)
    u8(1)
    u8(1)
    u8(0)
    u8(1)
    u8(1)

    u8(1)
    u8(DW_LNCT_PATH)
    u8(DW_FORM_STRING)
    u8(1)
    str("src")

    u8(3)
    u8(DW_LNCT_PATH)
    u8(DW_FORM_STRING)
    u8(DW_LNCT_TIMESTAMP)
    u8(DW_FORM_BLOCK)
    u8(DW_LNCT_SIZE)
    u8(DW_FORM_UDATA)
    u8(2)
    str("main.c")
    u8(3)
    u8(0xAA)
    u8(0xBB)
    u8(0xCC)
    u8(FIRST_FILE_SIZE)
    str("other.c")
    u8(1)
    u8(0xFF)
    u8(SECOND_FILE_SIZE)

    val payload = unit.toByteArray()
    val result = ByteArrayOutputStream()
    val length = payload.size
    result.write(length and 0xFF)
    result.write(length ushr 8 and 0xFF)
    result.write(length ushr 16 and 0xFF)
    result.write(length ushr 24 and 0xFF)
    result.write(payload)
    return result.toByteArray()
}
