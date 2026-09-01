package io.appmetrica.analytics.gradle.common.ndk.elf

@Suppress("LongParameterList")
class DebugElfSectionHeaders(
    val debugInfo: ElfSectionHeader,
    val debugAbbrev: ElfSectionHeader,
    val debugStr: ElfSectionHeader,
    val debugLine: ElfSectionHeader,
    val debugRanges: ElfSectionHeader?,
    val debugRnglists: ElfSectionHeader? = null,
    val debugAddr: ElfSectionHeader? = null,
    val debugStrOffsets: ElfSectionHeader? = null,
    val debugLineStr: ElfSectionHeader? = null
) {

    fun getHeaderNames() = listOfNotNull(
        debugInfo,
        debugAbbrev,
        debugStr,
        debugLine,
        debugRanges,
        debugRnglists,
        debugAddr,
        debugStrOffsets,
        debugLineStr
    ).map { it.nameString }

    companion object {

        @SuppressWarnings("ComplexCondition")
        fun from(elfHeaders: ElfSectionHeaders): DebugElfSectionHeaders? {
            val debugInfo = elfHeaders.getHeaderByName(ELF_SECTION_DEBUG_INFO)
            val debugAbbrev = elfHeaders.getHeaderByName(ELF_SECTION_DEBUG_ABBREV)
            val debugStr = elfHeaders.getHeaderByName(ELF_SECTION_DEBUG_STR)
            val debugLine = elfHeaders.getHeaderByName(ELF_SECTION_DEBUG_LINE)
            val debugRanges = elfHeaders.getHeaderByName(ELF_SECTION_DEBUG_RANGES)
            val debugRnglists = elfHeaders.getHeaderByName(ELF_SECTION_DEBUG_RNGLISTS)
            val debugAddr = elfHeaders.getHeaderByName(ELF_SECTION_DEBUG_ADDR)
            val debugStrOffsets = elfHeaders.getHeaderByName(ELF_SECTION_DEBUG_STR_OFFSETS)
            val debugLineStr = elfHeaders.getHeaderByName(ELF_SECTION_DEBUG_LINE_STR)
            return if (debugInfo != null && debugAbbrev != null && debugStr != null && debugLine != null) {
                DebugElfSectionHeaders(
                    debugInfo,
                    debugAbbrev,
                    debugStr,
                    debugLine,
                    debugRanges,
                    debugRnglists,
                    debugAddr,
                    debugStrOffsets,
                    debugLineStr
                )
            } else {
                null
            }
        }
    }
}
