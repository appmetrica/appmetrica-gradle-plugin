package io.appmetrica.analytics.gradle.common.ndk.dwarf.line.opcode

import io.appmetrica.analytics.gradle.common.ndk.dwarf.line.DebugLineContext

fun processSpecialOpcode(context: DebugLineContext, opcode: Int): Boolean {
    val adjustedOpcode = opcode - context.header.opcodeBase
    val operationAdvance = adjustedOpcode / context.header.lineRange
    val lineIncrement = context.header.lineBase + adjustedOpcode % context.header.lineRange

    context.registers.line += lineIncrement
    incrementAddressAndOpIndex(context, operationAdvance)
    context.registers.isBasicBlock = false
    context.registers.isPrologueEnd = false
    context.registers.isEpilogueBegin = false
    context.registers.discriminator = 0
    return true
}
