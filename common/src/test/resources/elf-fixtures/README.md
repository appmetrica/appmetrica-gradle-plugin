# ELF/DWARF test fixtures

The binary fixtures are stored in `common/src/test/resources/elf-fixtures` and
are available to tests through the test runtime classpath.

The directory contains the following Android ELF shared objects:

| File | ABI | Debug data | ELF `.symtab` | Purpose |
|---|---|---|---|---|
| `libdwarf4.so` | arm64-v8a | DWARF 4 | yes | NDK r27c default `-g` regression |
| `libdwarf5.so` | arm64-v8a | DWARF 5 | yes | NDK r27c `-gdwarf-5` and indexed forms/rnglists |
| `libdwarfmixed.so` | arm64-v8a | DWARF 4 + 5 | yes | Flutter 3.44-style mixed compilation units |
| `libdwarf4-arm.so` | armeabi-v7a | DWARF 4 | yes | 32-bit ARM and ARM mapping symbols |
| `libdwarf4-x86.so` | x86 | DWARF 4 | yes | 32-bit little-endian ELF |
| `libdwarf4-x86_64.so` | x86_64 | DWARF 4 | yes | non-ARM 64-bit ELF |
| `libelf-symbols-only.so` | arm64-v8a | none | yes | fallback to the ELF symbol table |
| `libdwarf-only.so` | arm64-v8a | DWARF 4 | no | extraction without a static symbol table |
| `libno-symbols.so` | arm64-v8a | none | no | valid shared object with no extractable symbols |
| `libbuild-id-segment-only.so` | arm64-v8a | DWARF 4 | yes | build ID available only through `PT_NOTE` |

When adding a new `.so` fixture, also add its path to the
[GitHub sync allowlist](https://nda.ya.ru/t/yAY9ZBlH7oE72S).
Otherwise the binary will not be published.

The DWARF 4/5/mixed libraries are fixed binary artifacts because compiler
upgrades can change their encoding and invalidate the regression coverage.

## Sources and directory layout

The checked-in `.so` files and this README live in
`common/src/test/resources/elf-fixtures`. Keeping the binaries in test
resources makes them available through the test runtime classpath, so running
the tests does not require an Android NDK or any environment variables.

The source files used to rebuild the binaries live outside test resources:

- `common/elf-fixtures/fixture.c` defines exported functions with branches,
  recursion, arguments, local variables, and source lines. It is compiled for
  every ABI and is the source of the regular DWARF 4 and DWARF 5 fixtures.
- `common/elf-fixtures/fixture_extra.c` defines a second compilation unit. It
  is compiled as DWARF 5 and linked with the DWARF 4 object produced from
  `fixture.c` to create `libdwarfmixed.so`.

These C files are intentionally small and dependency-free. Do not replace
them with production code: stable sources make changes in compiler-generated
ELF and DWARF data easier to identify. `rebuildElfFixtures` reads the sources
from `common/elf-fixtures`, builds all primary libraries, derives the
section-removal variants, and writes the final set into this directory.

## Rebuilding the fixtures

Use Android NDK r27c. A different compiler or linker version can produce valid
fixtures with different DWARF forms, so the rebuild task rejects every other
NDK revision.

If `ANDROID_HOME` points to an Android SDK containing
`ndk/27.2.12479018`, no additional configuration is required:

```shell
./gradlew :common:rebuildElfFixtures
```

`ANDROID_SDK_ROOT` is also supported as a fallback. An explicit NDK path takes
precedence over SDK auto-detection and can be supplied through
`ANDROID_NDK_HOME`:

```shell
ANDROID_NDK_HOME=/path/to/android-ndk-r27c \
  ./gradlew :common:rebuildElfFixtures
```

The NDK path can also be supplied as a Gradle property:

```shell
./gradlew :common:rebuildElfFixtures \
  -PandroidNdkPath=/path/to/android-ndk-r27c
```

Before changing any checked-in file, the task verifies:

- that an explicit NDK was supplied or r27c was found under `ANDROID_HOME`;
- that `source.properties` identifies exactly NDK r27c;
- that the host operating system is supported;
- that all required ARM, ARM64, x86, and x86_64 Clang wrappers exist;
- that all ten expected non-empty fixtures were generated.

All compilation and ELF transformations happen in the task's temporary
directory. Checked-in fixtures are replaced only after the complete set has
been built successfully. The task performs the section-header transformations
itself and does not require Python or host LLVM tools.

Run the fixture test after rebuilding:

```shell
./gradlew :common:test \
  --tests=io.appmetrica.analytics.gradle.common.ndk.ElfYSymFactoryFixturesSpek
```
