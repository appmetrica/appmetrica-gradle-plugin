package io.appmetrica.analytics.gradle.common.ndk

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File

object ElfYSymFactoryFixturesSpek : Spek({
    data class ExpectedSymbols(
        val elf: Boolean,
        val dwarf: Boolean
    )

    val expectedSymbolsByFixture = mapOf(
        "libbuild-id-segment-only.so" to ExpectedSymbols(elf = true, dwarf = true),
        "libdwarf-only.so" to ExpectedSymbols(elf = false, dwarf = true),
        "libdwarf4-arm.so" to ExpectedSymbols(elf = true, dwarf = true),
        "libdwarf4-x86.so" to ExpectedSymbols(elf = true, dwarf = true),
        "libdwarf4-x86_64.so" to ExpectedSymbols(elf = true, dwarf = true),
        "libdwarf4.so" to ExpectedSymbols(elf = true, dwarf = true),
        "libelf-symbols-only.so" to ExpectedSymbols(elf = true, dwarf = false),
        "libno-symbols.so" to ExpectedSymbols(elf = false, dwarf = false)
    )
    val disabledFixtures = setOf(
        // enable after DWARF 5 abbreviations are supported.
        "libdwarf5.so",
        // enable after mixed DWARF abbreviation tables are supported.
        "libdwarfmixed.so"
    )

    describe("ELF test fixtures") {
        val fixturesDir = File(
            requireNotNull(ElfYSymFactoryFixturesSpek::class.java.getResource("/elf-fixtures")) {
                "ELF fixtures resource directory does not exist"
            }.toURI()
        )

        fun fixture(name: String) = File(fixturesDir, name).also {
            require(it.isFile) { "ELF fixture does not exist: $it" }
        }

        it("has an expectation for every SO fixture") {
            val actualFixtures = requireNotNull(fixturesDir.listFiles()) {
                "Cannot list ELF fixtures directory: $fixturesDir"
            }.filter { it.isFile && it.extension == "so" }.map { it.name }.toSet()

            assertThat(actualFixtures).containsExactlyInAnyOrderElementsOf(
                expectedSymbolsByFixture.keys + disabledFixtures
            )
        }

        expectedSymbolsByFixture.forEach { (fixtureName, expectedSymbols) ->
            it("parses $fixtureName") {
                val parsingResult = runCatching {
                    ElfYSymFactory().createCSymFromFile(fixture(fixtureName))
                }
                val softly = SoftAssertions()

                softly.assertThat(parsingResult.exceptionOrNull())
                    .describedAs("%s should be parsed successfully", fixtureName)
                    .isNull()
                parsingResult.getOrNull()?.let { symbols ->
                    softly.assertThat(symbols.identifier)
                        .describedAs("%s build ID", fixtureName)
                        .isNotBlank()
                    softly.assertThat(symbols.elfSymbols.isNotEmpty())
                        .describedAs("%s ELF symbols presence", fixtureName)
                        .isEqualTo(expectedSymbols.elf)
                    softly.assertThat(symbols.compileUnits.isNotEmpty())
                        .describedAs("%s DWARF symbols presence", fixtureName)
                        .isEqualTo(expectedSymbols.dwarf)
                }
                softly.assertAll()
            }
        }
    }
})
