package io.appmetrica.analytics.gradle.common.ndk.io

import io.appmetrica.analytics.gradle.common.Log
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlin.math.max

private const val BUFFER_SIZE = 8192

@SuppressWarnings("TooManyFunctions")
class RandomAccessFileInputStream(file: File) : SeekableInputStream() {

    private var file: RandomAccessFile? = RandomAccessFile(file, "r")
    private val buffer = ByteArray(BUFFER_SIZE)
    private var filePointer = this.file!!.filePointer
    private var bufferPos = 0
    private var bufferCount = 0

    @Throws(IOException::class)
    override fun getCurrentOffset(): Long {
        validateOpen()
        val remaining = bufferCount - bufferPos
        return filePointer - max(remaining, 0)
    }

    @Throws(IOException::class)
    override fun seek(offset: Long) {
        validateOpen()
        if (offset < 0L) {
            throw IOException("Seek offset must be greater than 0")
        }
        val beginOffset = filePointer - bufferCount
        if (offset in beginOffset until filePointer) {
            bufferPos = (offset - beginOffset).toInt()
        } else {
            file!!.seek(offset)
            bufferCount = 0
            filePointer = file!!.filePointer
        }
    }

    @Throws(IOException::class)
    override fun readFully(buffer: ByteArray, offset: Int, length: Int) {
        validateOpen()
        var totalBytes = 0
        do {
            val bytesRead = read(buffer, offset + totalBytes, length - totalBytes)
            if (bytesRead <= 0) {
                Log.debug("offset = $offset, totalBytes = $totalBytes, length = $length, bytesRead = $bytesRead")
                break
            }
            totalBytes += bytesRead
        } while (totalBytes < length)
    }

    @Throws(IOException::class)
    override fun read(): Int {
        validateOpen()
        if (bufferPos >= bufferCount) {
            fillBuffer()
            if (bufferPos >= bufferCount) {
                return -1
            }
        }
        return buffer[bufferPos++].toInt() and MASK_FF
    }

    @Throws(IOException::class)
    override fun read(bytes: ByteArray): Int {
        validateOpen()
        return read(bytes, 0, bytes.size)
    }

    @Throws(IOException::class)
    override fun read(bytes: ByteArray, off: Int, len: Int): Int {
        if (off !in 0..bytes.size || len < 0 || off + len !in 0..bytes.size) {
            throw IndexOutOfBoundsException()
        }
        if (len == 0) {
            return len
        }
        validateOpen()
        var totalBytes = 0
        do {
            val bytesRead = readOnce(bytes, off + totalBytes, len - totalBytes)
            if (bytesRead <= 0) {
                Log.debug("totalBytes = $totalBytes, bytesRead = $bytesRead")
                if (totalBytes == 0) {
                    totalBytes = bytesRead
                }
                break
            }
            totalBytes += bytesRead
        } while (totalBytes < len)
        return totalBytes
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        if (n <= 0L) {
            return 0L
        }
        validateOpen()
        val remaining = bufferCount - bufferPos
        val skip: Long
        if (n <= remaining) {
            bufferPos += n.toInt()
            skip = n
        } else {
            val pos = getCurrentOffset()
            val fileLen = file!!.length()
            var newPos = pos + n
            if (newPos > fileLen) {
                newPos = fileLen
            }
            seek(newPos)
            skip = newPos - pos
        }
        return skip
    }

    @Throws(IOException::class)
    override fun close() {
        file?.close()
        file = null
    }

    @SuppressWarnings("ReturnCount")
    @Throws(IOException::class)
    private fun readOnce(bytes: ByteArray, offset: Int, len: Int): Int {
        var remaining = bufferCount - bufferPos
        if (remaining <= 0) {
            if (len >= buffer.size) {
                Log.debug("len = $len, buffer.size = ${buffer.size}, remaining = $remaining")
                return readFromFile(bytes, offset, len)
            }
            fillBuffer()
            remaining = bufferCount - bufferPos
            if (remaining <= 0) {
                Log.debug("remaining = $remaining")
                return -1
            }
        }
        val numBytesRead = if (remaining < len) remaining else len
        System.arraycopy(buffer, bufferPos, bytes, offset, numBytesRead)
        bufferPos += numBytesRead
        return numBytesRead
    }

    @Throws(IOException::class)
    private fun fillBuffer() {
        bufferPos = 0
        bufferCount = 0
        val numRead = readFromFile(buffer, 0, buffer.size)
        if (numRead > 0) {
            bufferCount = numRead
        }
    }

    @Throws(IOException::class)
    private fun readFromFile(bytes: ByteArray?, offset: Int, len: Int): Int {
        val bytesRead = file!!.read(bytes, offset, len)
        filePointer = file!!.filePointer
        return if (bytesRead < 0) 0 else bytesRead
    }

    @Throws(IOException::class)
    private fun validateOpen() {
        if (file == null) {
            throw IOException("Stream closed.")
        }
    }
}
