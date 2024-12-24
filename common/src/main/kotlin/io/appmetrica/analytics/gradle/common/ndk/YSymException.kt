package io.appmetrica.analytics.gradle.common.ndk

class YSymException : Exception {

    constructor(message: String) : super(message)

    constructor(detailMessage: String, throwable: Throwable) : super(detailMessage, throwable)
}
