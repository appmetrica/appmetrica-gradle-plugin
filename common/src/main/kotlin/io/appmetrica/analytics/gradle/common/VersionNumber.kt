package io.appmetrica.analytics.gradle.common

class VersionNumber private constructor(
    val major: Int,
    val minor: Int,
    val micro: Int,
    val patch: Int,
    private val scheme: AbstractScheme
) : Comparable<VersionNumber> {

    override fun compareTo(other: VersionNumber): Int {
        return if (this.major != other.major) {
            major - other.major
        } else if (this.minor != other.minor) {
            minor - other.minor
        } else if (this.micro != other.micro) {
            micro - other.micro
        } else {
            patch - other.patch
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is VersionNumber && this.compareTo(other) == 0
    }

    override fun hashCode(): Int {
        var result = this.major
        result = 31 * result + this.minor
        result = 31 * result + this.micro
        result = 31 * result + this.patch
        return result
    }

    override fun toString(): String {
        return scheme.format(this)
    }

    private class DefaultScheme : AbstractScheme(3) {
        override fun format(versionNumber: VersionNumber): String {
            return String.format(
                VERSION_TEMPLATE,
                versionNumber.major,
                versionNumber.minor,
                versionNumber.micro
            )
        }

        companion object {
            private const val VERSION_TEMPLATE = "%d.%d.%d"
        }
    }

    private abstract class AbstractScheme protected constructor(val depth: Int) : Scheme {
        override fun parse(value: String): VersionNumber {
            if (value.isNotEmpty()) {
                val scanner = Scanner(value)
                var minor = 0
                var micro = 0
                var patch = 0
                if (!scanner.hasDigit()) {
                    return UNKNOWN
                } else {
                    val major = scanner.scanDigit()
                    if (scanner.isSeparatorAndDigit('.')) {
                        scanner.skipSeparator()
                        minor = scanner.scanDigit()
                        if (scanner.isSeparatorAndDigit('.')) {
                            scanner.skipSeparator()
                            micro = scanner.scanDigit()
                            if (this.depth > 3 && scanner.isSeparatorAndDigit('.', '_')) {
                                scanner.skipSeparator()
                                patch = scanner.scanDigit()
                            }
                        }
                    }

                    if (scanner.isEnd) {
                        return VersionNumber(major, minor, micro, patch, this)
                    } else if (scanner.isQualifier) {
                        scanner.skipSeparator()
                        return VersionNumber(major, minor, micro, patch, this)
                    } else {
                        return UNKNOWN
                    }
                }
            } else {
                return UNKNOWN
            }
        }

        private class Scanner(val str: String) {
            var pos: Int = 0

            fun hasDigit(): Boolean {
                return this.pos < str.length && Character.isDigit(str[pos])
            }

            fun isSeparatorAndDigit(vararg separators: Char): Boolean {
                return this.pos < str.length - 1 && this.oneOf(*separators) && Character.isDigit(str[pos + 1])
            }

            private fun oneOf(vararg separators: Char): Boolean {
                val current = str[pos]

                for (i in separators.indices) {
                    val separator = separators[i]
                    if (current == separator) {
                        return true
                    }
                }

                return false
            }

            val isQualifier: Boolean
                get() = this.pos < str.length - 1 && this.oneOf('.', '-')

            fun scanDigit(): Int {
                val start = pos
                while (this.hasDigit()) {
                    ++this.pos
                }

                return str.substring(start, this.pos).toInt()
            }

            val isEnd: Boolean
                get() = this.pos == str.length

            fun skipSeparator() {
                ++this.pos
            }

            fun remainder(): String? {
                return if (this.pos == str.length) null else str.substring(this.pos)
            }
        }
    }

    interface Scheme {
        fun parse(value: String): VersionNumber

        fun format(versionNumber: VersionNumber): String
    }

    companion object {
        private val DEFAULT_SCHEME = DefaultScheme()
        private val UNKNOWN: VersionNumber = version(0)

        @JvmOverloads
        fun version(major: Int, minor: Int = 0): VersionNumber {
            return VersionNumber(major, minor, 0, 0, DEFAULT_SCHEME)
        }

        fun parse(versionString: String): VersionNumber {
            return DEFAULT_SCHEME.parse(versionString)
        }
    }
}
