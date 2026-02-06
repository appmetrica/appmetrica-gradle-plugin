package io.appmetrica.analytics.gradle.common.ndk.io

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.EOFException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

private const val BUFFER_SIZE = 64

@SuppressWarnings("TooManyFunctions")
class ByteReader(private val source: SeekableInputStream) : Closeable {

    private val bytes: ByteArray = ByteArray(BUFFER_SIZE)
    private val buffer: ByteBuffer = ByteBuffer.allocate(bytes.size)

    @Throws(IOException::class)
    override fun close() {
        source.close()
    }

    fun getCurrentOffset() = source.getCurrentOffset()

    fun getByteOrder() = buffer.order()!!

    fun setByteOrder(order: ByteOrder) {
        buffer.order(order)
    }

    @Throws(IOException::class)
    fun seek(offset: Long) {
        source.seek(offset)
    }

    @Throws(IOException::class)
    fun readBytes(numBytes: Int): ByteArray {
        val bytes = ByteArray(numBytes)
        source.readFully(bytes, 0, bytes.size)
        return bytes
    }

    @Throws(IOException::class)
    fun readByte(): Byte {
        val b = source.read()
        if (b < 0) {
            throw EOFException()
        }
        return (b and MASK_FF).toByte()
    }

    @Throws(IOException::class)
    fun readShort(numBytes: Int = Short.SIZE_BYTES): Short {
        buffer.put(readNumber(bytes, numBytes, Short.SIZE_BYTES, buffer.order()))
        buffer.flip()
        val answer = buffer.short
        buffer.clear()
        return answer
    }

    @Throws(IOException::class)
    fun readInt(numBytes: Int = Int.SIZE_BYTES): Int {
        buffer.put(readNumber(bytes, numBytes, Int.SIZE_BYTES, buffer.order()))
        buffer.flip()
        val answer = buffer.int
        buffer.clear()
        return answer
    }

    @Throws(IOException::class)
    fun readLong(numBytes: Int = Long.SIZE_BYTES): Long {
        buffer.put(readNumber(bytes, numBytes, Long.SIZE_BYTES, buffer.order()))
        buffer.flip()
        val answer = buffer.long
        buffer.clear()
        return answer
    }

    @Throws(IOException::class)
    fun readNullTerminatedString(charset: Charset): String {
        val bos = ByteArrayOutputStream()
        var b = source.read()
        while (b != 0) {
            if (b < 0) {
                throw EOFException()
            }
            bos.write(b)
            b = source.read()
        }
        return String(bos.toByteArray(), charset)
    }

    @Throws(IOException::class)
    fun readULEB128(): Int {
        var value = 0
        var shift = 0
        while (true) {
            val b = readByte().toInt()
            value = value or ((b and MASK_7F) shl shift)
            if ((b and MASK_80) == 0) {
                break
            }
            shift += Byte.SIZE_BITS - 1
        }
        return value
    }

    @Throws(IOException::class)
    fun readSLEB128(): Int {
        var value = 0
        var shift = 0
        var size = 0
        var b: Int
        do {
            b = readByte().toInt()
            ++size
            value = value or ((b and MASK_7F) shl shift)
            shift += Byte.SIZE_BITS - 1
        } while ((b and MASK_80) != 0)
        if (shift < size * Byte.SIZE_BITS && (b and MASK_40) != 0) {
            value = value or -(1 shl shift)
        }
        return value
    }

    @Throws(IOException::class)
    fun readBytesWithBlockSize() = readBytes(readULEB128())

    @Throws(IOException::class)
    fun readBytesWithBlockSize(numBytes: Int) = readBytes(readInt(numBytes))

    @Suppress("UseRequire")
    @Throws(IOException::class)
    private fun readNumber(buffer: ByteArray, numBytes: Int, width: Int, order: ByteOrder): ByteArray {
        if (numBytes > width) {
            throw IllegalArgumentException(
                "Requested number of bytes ($numBytes) was greater than available bytes ($width)."
            )
        }
        source.readFully(buffer, 0, numBytes)
        return padBytes(buffer, numBytes, width, order)
    }

    private fun padBytes(data: ByteArray, dataLen: Int, totalWidth: Int, order: ByteOrder): ByteArray {
        val padded = ByteArray(totalWidth)
        val dest = if (order == ByteOrder.BIG_ENDIAN) totalWidth - dataLen else 0
        System.arraycopy(data, 0, padded, dest, dataLen)
        return padded
    }
}
