package io.appmetrica.analytics.gradle.common.ndk.dwarf.info.range

import io.appmetrica.analytics.gradle.common.ndk.dwarf.SymbolNameProvider

data class NamedRange(val nameProvider: SymbolNameProvider, val start: Long, val end: Long) : Comparable<NamedRange> {

    operator fun contains(range: NamedRange) = start <= range.start && range.end <= end

    operator fun contains(address: Long) = address in start..end

    override fun compareTo(other: NamedRange) = start.compareTo(other.start)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NamedRange
        return start == other.start && end == other.end
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        return result
    }
}
