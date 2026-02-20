package io.appmetrica.analytics.gradle.common.ndk.dwarf.info.range

import io.appmetrica.analytics.gradle.common.ndk.dwarf.SymbolNameProvider
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import io.appmetrica.analytics.gradle.common.utils.Log
import java.io.IOException

class NamedRangesResolver(
    private val reader: ByteReader,
    private val addressSize: Int,
    private val rangesSectionOffset: Long
) {

    fun resolveNamedRanges(offset: Long, nameProvider: SymbolNameProvider, baseAddress: Long): List<NamedRange> {
        val originalOffset = reader.getCurrentOffset()
        try {
            val namedRanges = mutableListOf<NamedRange>()
            reader.seek(rangesSectionOffset + offset)
            var currentBaseAddress = baseAddress
            while (true) {
                var beginAddress = reader.readLong(addressSize)
                var endAddress = reader.readLong(addressSize)
                if (beginAddress == 0L && endAddress == 0L) {
                    break
                }
                if (beginAddress == -1L) {
                    currentBaseAddress = endAddress
                } else {
                    beginAddress += currentBaseAddress
                    endAddress += currentBaseAddress
                    namedRanges.add(NamedRange(nameProvider, beginAddress, endAddress))
                }
            }
            return namedRanges
        } catch (e: IOException) {
            Log.debug("Could not properly resolve range entries $e")
        } finally {
            reader.seek(originalOffset)
        }

        return emptyList()
    }
}
