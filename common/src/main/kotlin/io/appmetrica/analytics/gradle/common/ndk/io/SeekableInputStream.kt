package io.appmetrica.analytics.gradle.common.ndk.io

import java.io.IOException
import java.io.InputStream

abstract class SeekableInputStream : InputStream() {

    abstract fun getCurrentOffset(): Long

    @Throws(IOException::class)
    abstract fun seek(offset: Long)

    @Throws(IOException::class)
    abstract fun readFully(buffer: ByteArray, offset: Int, length: Int)
}
