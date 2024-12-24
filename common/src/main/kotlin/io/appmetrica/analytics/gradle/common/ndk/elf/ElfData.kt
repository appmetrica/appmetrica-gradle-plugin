package io.appmetrica.analytics.gradle.common.ndk.elf

import java.nio.ByteOrder

enum class ElfData(private val value: Int, val byteOrder: ByteOrder) {
    ELF_DATA_2LSB(1, ByteOrder.LITTLE_ENDIAN),
    ELF_DATA_2MSB(2, ByteOrder.BIG_ENDIAN);

    companion object {

        private val elfDataByValue = values().map { it.value to it }.toMap()

        @Throws(ElfException::class)
        fun fromValue(value: Int) = elfDataByValue.getOrElse(value) {
            throw ElfException("Invalid endianness: $value")
        }
    }
}
