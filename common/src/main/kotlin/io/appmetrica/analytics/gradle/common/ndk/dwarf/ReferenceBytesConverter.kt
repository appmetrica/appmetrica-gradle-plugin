package io.appmetrica.analytics.gradle.common.ndk.dwarf

import java.nio.ByteBuffer
import java.nio.ByteOrder

class ReferenceBytesConverter(private val byteOrder: ByteOrder) {

    fun asLongValue(data: ByteArray): Long {
        val padded = ByteArray(Long.SIZE_BYTES)
        val dest = if (byteOrder == ByteOrder.BIG_ENDIAN) Long.SIZE_BYTES - data.size else 0
        System.arraycopy(data, 0, padded, dest, data.size)
        return ByteBuffer.wrap(padded).order(byteOrder).long
    }
}
