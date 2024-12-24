package io.appmetrica.analytics.gradle.common.ndk.dwarf

import io.appmetrica.analytics.gradle.common.ndk.elf.DebugElfSectionHeaders

class FileContext(
    val debugHeaders: DebugElfSectionHeaders,
    val referenceBytesConverter: ReferenceBytesConverter,
    val namesMapByOffset: MutableMap<Long, SymbolNameProvider> = mutableMapOf()
)
