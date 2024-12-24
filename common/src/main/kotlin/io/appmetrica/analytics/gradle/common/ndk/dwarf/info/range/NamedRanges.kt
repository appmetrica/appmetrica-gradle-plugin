package io.appmetrica.analytics.gradle.common.ndk.dwarf.info.range

import java.util.TreeMap

class NamedRanges(namedRanges: List<NamedRange>) {

    private val rangesByStart = namedRanges.map { it.start to it }.toMap(TreeMap())

    fun rangeFor(address: Long) = rangesByStart.getEqualsOrLower(address)?.takeIf { address in it }
}

private fun <K, V> TreeMap<K, V>.getEqualsOrLower(key: K) = get(key) ?: lowerEntry(key)?.value
