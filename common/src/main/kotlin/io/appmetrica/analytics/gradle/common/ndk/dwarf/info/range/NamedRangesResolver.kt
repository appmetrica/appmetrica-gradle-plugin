package io.appmetrica.analytics.gradle.common.ndk.dwarf.info.range

import io.appmetrica.analytics.gradle.common.ndk.dwarf.SymbolNameProvider
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader

class NamedRangesResolver(
    private val reader: ByteReader,
    private val addressSize: Int,
    private val rangesSectionOffset: Long?,
    private val version: Int,
    private val resolveIndexedAddress: (Long) -> Long = { 0 }
) {

    fun resolveNamedRanges(offset: Long, nameProvider: SymbolNameProvider, baseAddress: Long): List<NamedRange> {
        if (rangesSectionOffset == null) {
            return emptyList()
        }
        return reader.resolveRangeList(
            addressSize,
            rangesSectionOffset + offset,
            baseAddress,
            version,
            resolveIndexedAddress
        ).map { NamedRange(nameProvider, it.first, it.second) }
    }
}
