package io.appmetrica.analytics.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.Properties
import javax.inject.Inject

private const val EXPECTED_NDK_REVISION = "27.2.12479018"
private const val ELF_SECTION_TYPE_SYMBOL_TABLE = 2

@Suppress("TooManyFunctions")
abstract class RebuildElfFixtures @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:Input
    @get:Optional
    abstract val ndkPath: Property<String>

    @get:Input
    @get:Optional
    abstract val androidHome: Property<String>

    @get:InputFile
    abstract val fixtureSource: RegularFileProperty

    @get:InputFile
    abstract val extraFixtureSource: RegularFileProperty

    @get:Internal
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    @Suppress("ThrowsCount")
    fun rebuild() {
        val ndk = findNdk()
        if (!ndk.isDirectory) {
            throw environmentError("Android NDK directory does not exist: $ndk")
        }
        verifyNdkVersion(ndk)

        val toolchain = ndk.resolve("toolchains/llvm/prebuilt/${hostToolchain()}/bin")
        val clang = mapOf(
            "aarch64" to toolchain.executable("aarch64-linux-android21-clang"),
            "arm" to toolchain.executable("armv7a-linux-androideabi21-clang"),
            "x86" to toolchain.executable("i686-linux-android21-clang"),
            "x86_64" to toolchain.executable("x86_64-linux-android21-clang")
        )
        clang.values.filterNot(File::isFile).takeIf(List<File>::isNotEmpty)?.let { missing ->
            throw environmentError(
                "Android NDK toolchain is incomplete. Missing executables:\n" +
                    missing.joinToString("\n") { "- $it" }
            )
        }

        val staging = temporaryDir.resolve("generated").apply {
            deleteRecursively()
            mkdirs()
        }
        val source = fixtureSource.get().asFile
        val extraSource = extraFixtureSource.get().asFile
        val commonFlags = listOf("-shared", "-fPIC", "-O0", "-g", "-Wl,--build-id=sha1")

        compileShared(clang.getValue("aarch64"), commonFlags, "-gdwarf-4", source, staging.resolve("libdwarf4.so"))
        compileShared(clang.getValue("arm"), commonFlags, "-gdwarf-4", source, staging.resolve("libdwarf4-arm.so"))
        compileShared(clang.getValue("x86"), commonFlags, "-gdwarf-4", source, staging.resolve("libdwarf4-x86.so"))
        compileShared(
            clang.getValue("x86_64"),
            commonFlags,
            "-gdwarf-4",
            source,
            staging.resolve("libdwarf4-x86_64.so")
        )
        compileShared(clang.getValue("aarch64"), commonFlags, "-gdwarf-5", source, staging.resolve("libdwarf5.so"))
        compileMixedDwarf(clang.getValue("aarch64"), source, extraSource, staging)
        createSectionRemovalFixtures(staging)
        verifyGeneratedFixtures(staging)

        val output = outputDirectory.get().asFile.apply { mkdirs() }
        expectedFixtureNames.forEach { name ->
            Files.copy(
                staging.resolve(name).toPath(),
                output.resolve(name).toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
        logger.lifecycle("Rebuilt ${expectedFixtureNames.size} ELF fixtures in $output")
    }

    @Suppress("ThrowsCount")
    private fun findNdk(): File {
        ndkPath.orNull?.takeIf(String::isNotBlank)?.let { return File(it) }

        val sdk = androidHome.orNull?.takeIf(String::isNotBlank)?.let(::File)
            ?: throw environmentError(
                "Android SDK path is not configured and no explicit NDK path was supplied.",
                "Set ANDROID_HOME, ANDROID_NDK_HOME, or pass " +
                    "-PandroidNdkPath=/path/to/android-ndk-r27c."
            )
        if (!sdk.isDirectory) {
            throw environmentError("Android SDK directory does not exist: $sdk")
        }

        val ndkRoot = sdk.resolve("ndk")
        val expected = ndkRoot.resolve(EXPECTED_NDK_REVISION)
        if (expected.isDirectory) return expected

        val installed = ndkRoot.listFiles()
            ?.filter(File::isDirectory)
            ?.map(File::getName)
            ?.sorted()
            .orEmpty()
        throw environmentError(
            "Android NDK r27c ($EXPECTED_NDK_REVISION) was not found in $ndkRoot.",
            "Installed NDK versions: ${installed.ifEmpty { listOf("none") }.joinToString()}",
            "Install it with the Android SDK Manager or supply its path explicitly."
        )
    }

    private fun verifyNdkVersion(ndk: File) {
        val sourceProperties = ndk.resolve("source.properties")
        if (!sourceProperties.isFile) {
            throw environmentError("Not an Android NDK directory: source.properties is missing in $ndk")
        }
        val revision = Properties().apply {
            sourceProperties.inputStream().use(::load)
        }.getProperty("Pkg.Revision")
        if (revision != EXPECTED_NDK_REVISION) {
            throw environmentError(
                "Unexpected Android NDK version.",
                "Expected: r27c ($EXPECTED_NDK_REVISION)",
                "Found: ${revision ?: "unknown"}",
                "Path: $ndk"
            )
        }
    }

    private fun compileShared(clang: File, flags: List<String>, dwarf: String, source: File, output: File) {
        runCommand(clang, flags + dwarf + source.absolutePath + listOf("-o", output.absolutePath))
    }

    private fun compileMixedDwarf(clang: File, source: File, extraSource: File, staging: File) {
        val dwarf4 = temporaryDir.resolve("dwarf4.o")
        val dwarf5 = temporaryDir.resolve("dwarf5.o")
        val compileFlags = listOf("-fPIC", "-O0", "-g")
        runCommand(clang, compileFlags + listOf("-gdwarf-4", "-c", source.absolutePath, "-o", dwarf4.absolutePath))
        runCommand(clang, compileFlags + listOf("-gdwarf-5", "-c", extraSource.absolutePath, "-o", dwarf5.absolutePath))
        runCommand(
            clang,
            listOf(
                "-shared", "-Wl,--build-id=sha1", dwarf4.absolutePath, dwarf5.absolutePath,
                "-o", staging.resolve("libdwarfmixed.so").absolutePath
            )
        )
    }

    private fun runCommand(executable: File, arguments: List<String>) {
        execOperations.exec {
            this.executable = executable.absolutePath
            args(arguments)
        }.assertNormalExitValue()
    }

    private fun createSectionRemovalFixtures(staging: File) {
        val dwarf4 = staging.resolve("libdwarf4.so")
        copyAndHideSections(dwarf4, staging.resolve("libelf-symbols-only.so"), prefixes = setOf(".debug", ".zdebug"))
        copyAndHideSections(dwarf4, staging.resolve("libdwarf-only.so"), names = setOf(".symtab"))
        copyAndHideSections(
            staging.resolve("libelf-symbols-only.so"),
            staging.resolve("libno-symbols.so"),
            names = setOf(".symtab")
        )
        copyAndHideSections(
            dwarf4,
            staging.resolve("libbuild-id-segment-only.so"),
            names = setOf(".note.gnu.build-id")
        )
    }

    @Suppress("MagicNumber")
    private fun copyAndHideSections(
        source: File,
        destination: File,
        names: Set<String> = emptySet(),
        prefixes: Set<String> = emptySet()
    ) {
        val data = source.readBytes()
        val headers = parseSectionHeaders(data)
        val selected = headers.filter { it.name in names || prefixes.any(it.name::startsWith) }
            .mapTo(mutableSetOf()) { it.index }
        headers.filter { it.index in selected && it.type == ELF_SECTION_TYPE_SYMBOL_TABLE }
            .forEach { selected += it.link }
        selected.forEach { index ->
            val position = headers[index].position
            data.fill(0, position, position + 8)
        }
        destination.writeBytes(data)
    }

    @Suppress("MagicNumber", "ThrowsCount", "MaxLineLength")
    private fun parseSectionHeaders(data: ByteArray): List<SectionHeader> {
        val elfMagic = byteArrayOf(0x7f, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())
        if (data.size < 64 || !data.copyOfRange(0, 4).contentEquals(elfMagic)) {
            throw GradleException("Generated fixture is not an ELF file")
        }
        if (data[5].toInt() != 1) {
            throw GradleException("Only little-endian ELF fixtures are supported")
        }
        val layout = when (data[4].toInt()) {
            1 -> SectionLayout(
                readUInt(data, 32, 4),
                readUInt(data, 46, 2),
                readUInt(data, 48, 2),
                readUInt(data, 50, 2),
                16,
                4
            )
            2 -> SectionLayout(
                readUInt(data, 40, 8),
                readUInt(data, 58, 2),
                readUInt(data, 60, 2),
                readUInt(data, 62, 2),
                24,
                8
            )
            else -> throw GradleException("Unsupported ELF class: ${data[4]}")
        }
        val headers = (0 until layout.count).map { index ->
            val position = layout.table + index * layout.entrySize
            SectionHeader(
                index = index,
                position = position,
                nameOffset = readUInt(data, position, 4),
                type = readUInt(data, position + 4, 4),
                offset = readUInt(data, position + layout.offsetField, layout.wordSize),
                size = readUInt(data, position + layout.offsetField + layout.wordSize, layout.wordSize),
                link = readUInt(data, position + layout.offsetField + layout.wordSize * 2, 4)
            )
        }
        val namesHeader = headers.getOrNull(layout.namesIndex)
            ?: throw GradleException("Invalid ELF section name table index: ${layout.namesIndex}")
        return headers.map { header ->
            val start = namesHeader.offset + header.nameOffset
            val limit = namesHeader.offset + namesHeader.size
            if (start !in namesHeader.offset until limit || limit > data.size) {
                throw GradleException("Invalid ELF section name offset: ${header.nameOffset}")
            }
            var end = start
            while (end < limit && data[end].toInt() != 0) end++
            header.copy(name = data.copyOfRange(start, end).toString(Charsets.US_ASCII))
        }
    }

    @Suppress("MagicNumber")
    private fun readUInt(data: ByteArray, offset: Int, size: Int): Int {
        if (offset < 0 || size !in 1..8 || offset + size > data.size) {
            throw GradleException("Invalid ELF offset: offset=$offset, size=$size")
        }
        var result = 0L
        for (index in 0 until size) {
            result = result or ((data[offset + index].toLong() and 0xff) shl (index * 8))
        }
        if (result > Int.MAX_VALUE) {
            throw GradleException("ELF value is too large: $result")
        }
        return result.toInt()
    }

    private fun hostToolchain(): String = when {
        System.getProperty("os.name").lowercase(Locale.ROOT).contains("mac") -> "darwin-x86_64"
        System.getProperty("os.name").lowercase(Locale.ROOT).contains("linux") -> "linux-x86_64"
        System.getProperty("os.name").lowercase(Locale.ROOT).contains("windows") -> "windows-x86_64"
        else -> throw environmentError("Unsupported host operating system: ${System.getProperty("os.name")}")
    }

    private fun File.executable(name: String): File = resolve(
        if (System.getProperty("os.name").lowercase(Locale.ROOT).contains("windows")) "$name.cmd" else name
    )

    private fun verifyGeneratedFixtures(staging: File) {
        val actual = staging.listFiles()?.filter(File::isFile)?.map(File::getName)?.toSet().orEmpty()
        if (actual != expectedFixtureNames) {
            throw GradleException(
                "Unexpected generated fixtures. Expected $expectedFixtureNames, got $actual"
            )
        }
        staging.listFiles().orEmpty().filter(File::isFile).forEach { file ->
            if (file.length() == 0L) throw GradleException("Generated fixture is empty: ${file.name}")
        }
    }

    private fun environmentError(vararg lines: String) = GradleException(
        (listOf("Cannot rebuild ELF fixtures.", "") + lines).joinToString("\n")
    )

    private data class SectionLayout(
        val table: Int,
        val entrySize: Int,
        val count: Int,
        val namesIndex: Int,
        val offsetField: Int,
        val wordSize: Int
    )

    private data class SectionHeader(
        val index: Int,
        val position: Int,
        val nameOffset: Int,
        val type: Int,
        val offset: Int,
        val size: Int,
        val link: Int,
        val name: String = ""
    )

    private companion object {
        val expectedFixtureNames = setOf(
            "libbuild-id-segment-only.so",
            "libdwarf-only.so",
            "libdwarf4-arm.so",
            "libdwarf4-x86.so",
            "libdwarf4-x86_64.so",
            "libdwarf4.so",
            "libdwarf5.so",
            "libdwarfmixed.so",
            "libelf-symbols-only.so",
            "libno-symbols.so"
        )
    }
}
