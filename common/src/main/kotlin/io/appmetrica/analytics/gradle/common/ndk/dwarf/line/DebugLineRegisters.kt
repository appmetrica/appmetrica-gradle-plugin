package io.appmetrica.analytics.gradle.common.ndk.dwarf.line

class DebugLineRegisters(private val defaultIsStatement: Boolean) {

    var address: Long = 0
    var opIndex: Int = 0
    var file: Int = 1
    var line: Long = 1
    var column: Long = 0
    var isStatement: Boolean = defaultIsStatement
    var isBasicBlock: Boolean = false
    var isEndSequence: Boolean = false
    var isPrologueEnd: Boolean = false
    var isEpilogueBegin: Boolean = false
    var isa: Long = 0
    var discriminator: Long = 0

    init {
        reset()
    }

    fun reset() {
        address = 0
        opIndex = 0
        file = 1
        line = 1
        column = 0
        isStatement = defaultIsStatement
        isBasicBlock = false
        isEndSequence = false
        isPrologueEnd = false
        isEpilogueBegin = false
        isa = 0
        discriminator = 0
    }
}
