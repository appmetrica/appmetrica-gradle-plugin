package io.appmetrica.analytics.gradle.common.ndk

import java.io.File
import java.io.IOException

interface YSymFactory {

    @Throws(YSymException::class, IOException::class)
    fun createCSymFromFile(binFile: File): YSym
}
