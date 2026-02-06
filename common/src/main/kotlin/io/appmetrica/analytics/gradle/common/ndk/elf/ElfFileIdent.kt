package io.appmetrica.analytics.gradle.common.ndk.elf

import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

private const val EI_NIDENT = 16

@SuppressWarnings("MagicNumber")
private val magicNumber = "${0x7f.toChar()}ELF".toByteArray()
private const val EI_CLASS = 4
private const val EI_DATA = 5
private const val EI_VERSION = 6
private const val EI_OSABI = 7
private const val EI_ABIVERSION = 8

class ElfFileIdent(
    val elfClass: ElfClass,
    val elfData: ElfData,
    val elfVersion: Int,
    val osAbi: Int,
    val abiVersion: Int
)

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Throws(IOException::class, ElfException::class)
fun ByteReader.readElfFileIdent() = readBytes(EI_NIDENT).let { ident ->
    if (magicNumber.contentEquals(ident.copyOf(magicNumber.size)) == false) {
        throw ElfException("Invalid magic number for file")
    }
    val version = ident[EI_VERSION].toInt()
    if (version != 1) {
        throw ElfException("Invalid ELF version: $version")
    }
    ElfFileIdent(
        elfClass = ElfClass.fromValue(ident[EI_CLASS].toInt()),
        elfData = ElfData.fromValue(ident[EI_DATA].toInt()),
        elfVersion = ident[EI_VERSION].toInt(),
        osAbi = ident[EI_OSABI].toInt(),
        abiVersion = ident[EI_ABIVERSION].toInt()
    )
}
/* ktlint-enable appmetrica-rules:no-top-level-members */
