package io.appmetrica.analytics.gradle.common.ndk.dwarf.line.opcode

import io.appmetrica.analytics.gradle.common.ndk.dwarf.DwarfException
import io.appmetrica.analytics.gradle.common.ndk.dwarf.line.DebugLineContext
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

private const val EXTENDED_OPCODE = 0

@Throws(IOException::class, DwarfException::class)
fun processOpcode(reader: ByteReader, context: DebugLineContext): Boolean {
    val opcode = reader.readInt(Byte.SIZE_BYTES)
    if (opcode < 0) {
        throw DwarfException("Could not process opcode $opcode")
    }
    return when {
        opcode >= context.header.opcodeBase -> processSpecialOpcode(context, opcode)
        opcode == EXTENDED_OPCODE -> {
            val length = reader.readULEB128()
            val exOpcode = reader.readInt(Byte.SIZE_BYTES)
            processExtendedOpcode(reader, context, exOpcode)
        }
        else -> processStandardOpcode(reader, context, opcode)
    }
}

fun incrementAddressAndOpIndex(context: DebugLineContext, operationAdvance: Int) {
    context.registers.address += context.header.minInstructionLength *
        ((context.registers.opIndex + operationAdvance) / context.header.maxOperationsPerInstruction)
    context.registers.opIndex = (context.registers.opIndex + operationAdvance) %
        context.header.maxOperationsPerInstruction
}
