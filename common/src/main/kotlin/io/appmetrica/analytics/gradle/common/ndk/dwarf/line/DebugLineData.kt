package io.appmetrica.analytics.gradle.common.ndk.dwarf.line

class DebugLineData(
    val debugLineEntries: List<DebugLineEntry> = emptyList(),
    val debugLineContext: DebugLineContext? = null
)
