package io.appmetrica.analytics.gradle.common.ndk.elf

class DebugElfSectionHeaders(
    val debugInfo: ElfSectionHeader,
    val debugAbbrev: ElfSectionHeader,
    val debugStr: ElfSectionHeader,
    val debugLine: ElfSectionHeader,
    val debugRanges: ElfSectionHeader?
) {

    fun getHeaderNames() = listOfNotNull(
        debugInfo,
        debugAbbrev,
        debugStr,
        debugLine,
        debugRanges
    ).map { it.nameString }

    companion object {

        @SuppressWarnings("ComplexCondition")
        fun from(elfHeaders: ElfSectionHeaders): DebugElfSectionHeaders? {
            val debugInfo = elfHeaders.getHeaderByName(ELF_SECTION_DEBUG_INFO)
            val debugAbbrev = elfHeaders.getHeaderByName(ELF_SECTION_DEBUG_ABBREV)
            val debugStr = elfHeaders.getHeaderByName(ELF_SECTION_DEBUG_STR)
            val debugLine = elfHeaders.getHeaderByName(ELF_SECTION_DEBUG_LINE)
            val debugRanges = elfHeaders.getHeaderByName(ELF_SECTION_DEBUG_RANGES)
            return if (debugInfo != null && debugAbbrev != null && debugStr != null && debugLine != null) {
                DebugElfSectionHeaders(debugInfo, debugAbbrev, debugStr, debugLine, debugRanges)
            } else {
                null
            }
        }
    }
}
