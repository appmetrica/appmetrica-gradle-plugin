package io.appmetrica.analytics.gradle.common.ndk.io

import java.io.EOFException

internal class ByteArraySeekableInputStream(private val data: ByteArray) : SeekableInputStream() {

    private var position = 0

    override fun getCurrentOffset() = position.toLong()

    override fun seek(offset: Long) {
        if (offset < 0 || offset > data.size) {
            throw EOFException()
        }
        position = offset.toInt()
    }

    override fun readFully(buffer: ByteArray, offset: Int, length: Int) {
        if (position + length > data.size) {
            throw EOFException()
        }
        System.arraycopy(data, position, buffer, offset, length)
        position += length
    }

    override fun read(): Int {
        if (position >= data.size) {
            return -1
        }
        return data[position++].toInt() and MASK_FF
    }
}
