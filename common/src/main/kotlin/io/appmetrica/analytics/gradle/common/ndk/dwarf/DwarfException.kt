package io.appmetrica.analytics.gradle.common.ndk.dwarf

class DwarfException : Exception {

    constructor() : super()

    constructor(message: String) : super(message)

    constructor(message: String, throwable: Throwable) : super(message, throwable)

    constructor(throwable: Throwable) : super(throwable)
}
