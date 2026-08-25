package io.appmetrica.analytics.gradle.common

import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration

object HttpSystemTimeoutsSpek : Spek({
    describe("Apache-compatible HTTP connect timeout system property") {
        val propertyKey = HttpSystemTimeouts.CONNECTION_TIMEOUT_PROPERTY
        var saved: String? = null

        beforeEachTest {
            saved = System.getProperty(propertyKey)
            System.clearProperty(propertyKey)
        }

        afterEachTest {
            if (saved == null) System.clearProperty(propertyKey) else System.setProperty(propertyKey, saved)
            saved = null
        }

        it("reads connection timeout in millis; ignores unset/invalid") {
            assertThat(HttpSystemTimeouts.connectTimeout()).isNull()

            System.setProperty(propertyKey, "1500")
            assertThat(HttpSystemTimeouts.connectTimeout()).isEqualTo(Duration.ofMillis(1500))

            System.setProperty(propertyKey, "0")
            assertThat(HttpSystemTimeouts.connectTimeout()).isEqualTo(Duration.ZERO)

            System.setProperty(propertyKey, "abc")
            assertThat(HttpSystemTimeouts.connectTimeout()).isNull()

            System.setProperty(propertyKey, "-1")
            assertThat(HttpSystemTimeouts.connectTimeout()).isNull()
        }
    }
})
