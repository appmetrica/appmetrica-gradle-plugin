package io.appmetrica.analytics.gradle.common.ndk.dwarf.line.opcode

import io.appmetrica.analytics.gradle.common.ndk.dwarf.line.DebugLineContext
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

private const val CONST_OPCODE = 255
private const val NUMBER_OF_STANDARD_OPCODE = 12

@SuppressWarnings("MagicNumber")
enum class StandardOpcode(private val value: Int, val isNewRow: Boolean = false) {
    COPY(1, true),
    ADVANCE_PC(2),
    ADVANCE_LINE(3),
    SET_FILE(4),
    SET_COLUMN(5),
    NEGATE_STATEMENT(6),
    SET_BASIC_BLOCK(7),
    CONST_ADD_PC(8),
    FIXED_ADVANCE_PC(9),
    SET_PROLOGUE_END(10),
    SET_EPILOGUE_BEGIN(11),
    SET_ISA(12);

    companion object {

        private val opcodeByValue = values().map { it.value to it }.toMap()

        fun fromValue(value: Int) = opcodeByValue.getValue(value)
    }
}

/* ktlint-disable appmetrica-rules:no-top-level-members */
@SuppressWarnings("ComplexMethod")
@Throws(IOException::class)
fun processStandardOpcode(reader: ByteReader, context: DebugLineContext, opcode: Int): Boolean {
    if (opcode > NUMBER_OF_STANDARD_OPCODE) {
        reader.readBytes(context.header.standardOpcodeLengths[opcode].toInt())
        return false
    }
    val standardOpcode = StandardOpcode.fromValue(opcode)
    when (standardOpcode) {
        StandardOpcode.COPY -> {
            context.registers.discriminator = 0L
            context.registers.isBasicBlock = false
            context.registers.isPrologueEnd = false
            context.registers.isEpilogueBegin = false
        }
        StandardOpcode.ADVANCE_PC -> {
            incrementAddressAndOpIndex(context, reader.readULEB128())
        }
        StandardOpcode.ADVANCE_LINE -> {
            context.registers.line += reader.readSLEB128().toLong()
        }
        StandardOpcode.SET_FILE -> {
            context.registers.file = reader.readULEB128()
        }
        StandardOpcode.SET_COLUMN -> {
            context.registers.column = reader.readULEB128().toLong()
        }
        StandardOpcode.NEGATE_STATEMENT -> {
            context.registers.isStatement = !context.registers.isStatement
        }
        StandardOpcode.SET_BASIC_BLOCK -> {
            context.registers.isBasicBlock = true
        }
        StandardOpcode.CONST_ADD_PC -> {
            val adjustedOpcode = CONST_OPCODE - context.header.opcodeBase
            val operationAdvance = adjustedOpcode / context.header.lineRange
            incrementAddressAndOpIndex(context, operationAdvance)
        }
        StandardOpcode.FIXED_ADVANCE_PC -> {
            context.registers.address += reader.readLong(Short.SIZE_BYTES)
            context.registers.opIndex = 0
        }
        StandardOpcode.SET_PROLOGUE_END -> {
            context.registers.isPrologueEnd = true
        }
        StandardOpcode.SET_EPILOGUE_BEGIN -> {
            context.registers.isEpilogueBegin = true
        }
        StandardOpcode.SET_ISA -> {
            context.registers.isa = reader.readULEB128().toLong()
        }
    }
    return standardOpcode.isNewRow
}
/* ktlint-enable appmetrica-rules:no-top-level-members */
