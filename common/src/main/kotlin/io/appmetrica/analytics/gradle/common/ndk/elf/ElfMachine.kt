package io.appmetrica.analytics.gradle.common.ndk.elf

@SuppressWarnings("MagicNumber")
enum class ElfMachine(private val value: Int, val archName: String) {
    EM_SPARC(0x2, "sparc"),
    EM_386(0x3, "x86"),
    EM_MIPS(0x8, "mips"),
    EM_ARM(0x28, "arm"),
    EM_X86_64(0x3e, "x86_64"),
    EM_AARCH64(0xb7, "aarch64");

    companion object {

        private val machineByValue = values().map { it.value to it }.toMap()

        @Throws(ElfException::class)
        fun fromValue(value: Int) = machineByValue.getOrElse(value) {
            throw ElfException("Unrecognized ELF machine architecture: ${"0x%x".format(value)}")
        }
    }
}
