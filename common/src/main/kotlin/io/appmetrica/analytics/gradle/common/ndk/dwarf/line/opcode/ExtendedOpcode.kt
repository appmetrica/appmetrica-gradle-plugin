package io.appmetrica.analytics.gradle.common.ndk.dwarf.line.opcode

import io.appmetrica.analytics.gradle.common.ndk.dwarf.line.DebugLineContext
import io.appmetrica.analytics.gradle.common.ndk.io.ByteReader
import java.io.IOException

@SuppressWarnings("MagicNumber")
enum class ExtendedOpcode(private val value: Int) {
    END_SEQUENCE(1),
    SET_ADDRESS(2),
    DEFINE_FILE(3),
    SET_DISCRIMINATOR(4);

    companion object {

        private val opcodeByValue = values().map { it.value to it }.toMap()

        fun fromValue(value: Int) = opcodeByValue.getValue(value)
    }
}

@Throws(IOException::class)
fun processExtendedOpcode(reader: ByteReader, context: DebugLineContext, opcode: Int): Boolean {
    when (ExtendedOpcode.fromValue(opcode)) {
        ExtendedOpcode.END_SEQUENCE -> {
            context.registers.isEndSequence = true
        }
        ExtendedOpcode.SET_ADDRESS -> {
            context.registers.address = reader.readLong(context.offsetSize)
            context.registers.opIndex = 0
        }
        ExtendedOpcode.DEFINE_FILE -> {
            context.defineFile(
                reader.readNullTerminatedString(Charsets.UTF_8),
                reader.readULEB128(),
                reader.readULEB128(),
                reader.readULEB128()
            )
        }
        ExtendedOpcode.SET_DISCRIMINATOR -> {
            context.registers.discriminator = reader.readULEB128().toLong()
        }
    }
    return false
}
