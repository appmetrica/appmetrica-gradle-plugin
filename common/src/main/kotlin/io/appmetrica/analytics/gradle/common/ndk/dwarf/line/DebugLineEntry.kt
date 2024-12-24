package io.appmetrica.analytics.gradle.common.ndk.dwarf.line

class DebugLineEntry(
    val address: Long,
    val file: Int,
    val lineNumber: Long,
    val columnNumber: Long,
    val endSequence: Boolean
)
