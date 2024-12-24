package io.appmetrica.analytics.gradle.common.ndk.elf

@SuppressWarnings("MagicNumber")
enum class ElfClass(private val value: Int, val wordSize: Int) {
    ELF_CLASS_32(1, 4),
    ELF_CLASS_64(2, 8);

    companion object {

        private val elfClassByValue = values().map { it.value to it }.toMap()

        @Throws(ElfException::class)
        fun fromValue(value: Int) = elfClassByValue.getOrElse(value) {
            throw ElfException("Invalid addresses format: $value")
        }
    }
}
