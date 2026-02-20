package io.appmetrica.analytics.gradle.common.ndk.elf

import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import io.appmetrica.analytics.gradle.common.utils.Log
import java.io.IOException

private val armArch = arrayOf(
    "Pre-v4", "4", "4T", "5T", "5TE", "5TEJ", "6", "6KZ", "6T2", "6K", "7", "6-M", "6S-M", "7E-M", "8"
)
private const val ARM_FORMAT_VERSION = 'A'.toByte()
private const val ARM_AEABI_ATTRIBUTES_SUBSECTION = "aeabi"
private const val ARM_TAG_FILE_ATTRIBUTE = 1.toByte()

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Throws(IOException::class)
fun parseArmVersion(reader: ByteReader, elfFileHeader: ElfFileHeader, sectionHeaders: ElfSectionHeaders) =
    if (elfFileHeader.machine == ElfMachine.EM_ARM) {
        sectionHeaders.findHeader { it.isArmAttributes() }?.run {
            parseArmVersionFromArmAttributesSection(reader, offset, size)
        }
    } else {
        null
    }
/* ktlint-enable appmetrica-rules:no-top-level-members */

@Suppress("UseRequire")
@Throws(IOException::class)
private fun parseArmVersionFromArmAttributesSection(reader: ByteReader, sectionOffset: Long, size: Long): String? {
    reader.seek(sectionOffset)
    val version = reader.readByte()
    if (version != ARM_FORMAT_VERSION) {
        throw IllegalArgumentException("Invalid arm format-version")
    }
    var dataRemaining = size - 1
    while (dataRemaining > 0) {
        var sectionRemaining = reader.readLong(Int.SIZE_BYTES)
        if (sectionRemaining > dataRemaining) {
            throw IOException("Section size $sectionRemaining is greater than remaining section length $dataRemaining.")
        }
        dataRemaining -= sectionRemaining
        sectionRemaining -= Int.SIZE_BYTES
        val sectionName = reader.readNullTerminatedString(Charsets.UTF_8)
        sectionRemaining -= (sectionName.length - 1)
        if (sectionName == ARM_AEABI_ATTRIBUTES_SUBSECTION) {
            return parseArmVersionFromAeabiAttributesSubsection(reader, sectionRemaining)
        }
        reader.seek(reader.getCurrentOffset() + sectionRemaining)
    }
    Log.debug("Did not find an ARM aeabi attributes subsection.")
    return null
}

@Throws(IOException::class)
private fun parseArmVersionFromAeabiAttributesSubsection(reader: ByteReader, sectionRemaining: Long): String? {
    var sectionRemaining = sectionRemaining
    while (sectionRemaining > 0L) {
        val tag = reader.readByte()
        var aeabiSectionRemaining = reader.readLong(Int.SIZE_BYTES)
        if (aeabiSectionRemaining > sectionRemaining) {
            throw IOException(
                "Subsection size $aeabiSectionRemaining is greater than parent section size $sectionRemaining."
            )
        }
        sectionRemaining -= aeabiSectionRemaining
        aeabiSectionRemaining -= 1 + Int.SIZE_BYTES
        if (tag == ARM_TAG_FILE_ATTRIBUTE) {
            return parseArmVersionFromTagFileAttribute(reader, aeabiSectionRemaining)
        }
        reader.seek(reader.getCurrentOffset() + aeabiSectionRemaining)
    }
    Log.debug("Did not find an ARM file attribute subsection.")
    return null
}

@SuppressWarnings("MagicNumber") // TODO https://nda.ya.ru/t/JfWmp0A379dNNG
@Throws(IOException::class)
private fun parseArmVersionFromTagFileAttribute(reader: ByteReader, aeabiSectionRemaining: Long): String? {
    val nextSubSection = reader.getCurrentOffset() + aeabiSectionRemaining
    while (reader.getCurrentOffset() < nextSubSection) {
        val attrTag = reader.readULEB128()
        when (attrTag) {
            4, 5, 32, 65, 67 -> reader.readNullTerminatedString(Charsets.UTF_8)
            6 -> return armArch[reader.readULEB128()]
            else -> reader.readULEB128()
        }
    }
    Log.debug("Did not find an ARM architecture field.")
    return null
}
