package io.appmetrica.analytics.gradle.common.utils

import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object VersionNumberSpek : Spek({
    describe("parse") {
        it("parses standard version string") {
            val version = VersionNumber.parse("7.2.3")
            assertThat(version.major).isEqualTo(7)
            assertThat(version.minor).isEqualTo(2)
            assertThat(version.micro).isEqualTo(3)
        }

        it("parses version with only major and minor") {
            val version = VersionNumber.parse("8.1")
            assertThat(version.major).isEqualTo(8)
            assertThat(version.minor).isEqualTo(1)
            assertThat(version.micro).isEqualTo(0)
        }

        it("parses version with qualifier") {
            val version = VersionNumber.parse("7.2.0-beta01")
            assertThat(version.major).isEqualTo(7)
            assertThat(version.minor).isEqualTo(2)
            assertThat(version.micro).isEqualTo(0)
        }

        it("returns 0.0.0 for empty string") {
            val version = VersionNumber.parse("")
            assertThat(version).isEqualTo(VersionNumber.version(0, 0))
        }

        it("returns 0.0.0 for non-numeric string") {
            val version = VersionNumber.parse("abc")
            assertThat(version).isEqualTo(VersionNumber.version(0, 0))
        }
    }

    describe("compareTo") {
        it("compares by major version") {
            assertThat(VersionNumber.parse("8.0.0")).isGreaterThan(VersionNumber.parse("7.0.0"))
        }

        it("compares by minor when major is equal") {
            assertThat(VersionNumber.parse("7.3.0")).isGreaterThan(VersionNumber.parse("7.2.0"))
        }

        it("compares by micro when major and minor are equal") {
            assertThat(VersionNumber.parse("7.2.3")).isGreaterThan(VersionNumber.parse("7.2.1"))
        }

        it("treats equal versions as equal") {
            assertThat(VersionNumber.parse("7.2.3")).isEqualTo(VersionNumber.parse("7.2.3"))
        }
    }

    describe("version factory") {
        it("creates version with major only") {
            val version = VersionNumber.version(8)
            assertThat(version.major).isEqualTo(8)
            assertThat(version.minor).isEqualTo(0)
        }

        it("creates version with major and minor") {
            val version = VersionNumber.version(8, 1)
            assertThat(version.major).isEqualTo(8)
            assertThat(version.minor).isEqualTo(1)
        }
    }

    describe("equals and hashCode") {
        it("equal versions have same hashCode") {
            val v1 = VersionNumber.parse("7.2.3")
            val v2 = VersionNumber.parse("7.2.3")
            assertThat(v1.hashCode()).isEqualTo(v2.hashCode())
        }

        it("different versions are not equal") {
            assertThat(VersionNumber.parse("7.2.3")).isNotEqualTo(VersionNumber.parse("7.2.4"))
        }
    }

    describe("toString") {
        it("formats version as major.minor.micro") {
            assertThat(VersionNumber.parse("7.2.3").toString()).isEqualTo("7.2.3")
        }
    }
})
