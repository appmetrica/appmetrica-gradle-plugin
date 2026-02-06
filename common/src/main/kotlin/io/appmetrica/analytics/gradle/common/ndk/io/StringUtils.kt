package io.appmetrica.analytics.gradle.common.ndk.io

import com.google.gson.GsonBuilder
import io.appmetrica.analytics.gradle.common.ndk.elf.ElfFileHeader
import io.appmetrica.analytics.gradle.common.ndk.elf.ElfSectionHeaders
import io.appmetrica.analytics.gradle.common.ndk.elf.ElfSymbol

private const val HEX = 16

/* ktlint-disable appmetrica-rules:no-top-level-members */
fun ByteArray.toPrettyString() = joinToString(" ") { "%02x".format(it) }

fun ElfFileHeader.toPrettyString() = """
    {
      "ident": {
        "elfClass": "${ident.elfClass}",
        "elfData": "${ident.elfData}",
        "elfVersion": ${ident.elfVersion.toString(HEX)},
        "osAbi": ${ident.osAbi.toString(HEX)},
        "abiVersion": ${ident.abiVersion.toString(HEX)}
      },
      "type": ${type.toString(HEX)},
      "machine": "$machine",
      "version": ${version.toString(HEX)},
      "entry": ${entry.toString(HEX)},
      "programHeaderOffset": ${programHeaderOffset.toString(HEX)},
      "sectionHeaderOffset": ${sectionHeaderOffset.toString(HEX)},
      "flags": ${flags.toString(HEX)},
      "elfHeaderSize": $elfHeaderSize,
      "programHeaderEntrySize": $programHeaderEntrySize,
      "programHeaderNum": ${programHeaderNum.toString(HEX)},
      "sectionHeaderEntrySize": $sectionHeaderEntrySize,
      "sectionHeaderNum": ${sectionHeaderNum.toString(HEX)},
      "sectionHeaderStringIndex": ${sectionHeaderStringIndex.toString(HEX)}
    }
""".trimIndent()

fun <T> T.toPrettyString(): String = GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(this)

fun ElfSectionHeaders.print(printer: (String) -> Unit) = Table(
    headersByName.map { (_, header) ->
        mapOf(
            "Name string" to header.nameString,
            "Name" to header.name.toString(HEX),
            "Type" to header.type.toString(HEX),
            "Flags" to header.flags.toString(HEX),
            "Address" to header.address.toString(HEX),
            "Offset" to header.offset.toString(HEX),
            "Size" to header.size,
            "Link" to header.link.toString(HEX),
            "Info" to header.info.toString(HEX),
            "Address align" to header.addressAlign.toString(HEX),
            "Entry size" to header.entrySize.toString(HEX)
        )
    }
).print(printer)

fun List<ElfSymbol>.print(printer: (String) -> Unit) = Table(
    map { symbol ->
        mapOf(
            "Name string" to symbol.nameString,
            "Name" to symbol.name.toString(HEX),
            "Value" to symbol.value.toString(HEX),
            "Size" to symbol.size,
            "Info" to symbol.info.toString(HEX),
            "Other" to symbol.other.toString(HEX),
            "Index" to symbol.sectionTableIndex.toString(HEX)
        )
    }
).print(printer)
/* ktlint-enable appmetrica-rules:no-top-level-members */
