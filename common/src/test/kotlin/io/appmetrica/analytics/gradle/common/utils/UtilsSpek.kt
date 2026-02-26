package io.appmetrica.analytics.gradle.common.utils

import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object UtilsSpek : Spek({
    describe("uppercaseFirstChar") {
        it("uppercases first character of lowercase string") {
            assertThat("release".uppercaseFirstChar()).isEqualTo("Release")
        }

        it("keeps already uppercase first character") {
            assertThat("Release".uppercaseFirstChar()).isEqualTo("Release")
        }

        it("handles single character string") {
            assertThat("r".uppercaseFirstChar()).isEqualTo("R")
        }

        it("handles empty string") {
            assertThat("".uppercaseFirstChar()).isEqualTo("")
        }
    }
})
