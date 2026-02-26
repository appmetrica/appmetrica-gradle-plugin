package io.appmetrica.analytics.gradle.common.utils

import org.assertj.core.api.Assertions.assertThatCode
import org.gradle.api.logging.Logger
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object LogSpek : Spek({
    describe("Log") {
        it("does not throw when logger is not set") {
            // Reset to ensure no logger
            val field = Log::class.java.getDeclaredField("logger")
            field.isAccessible = true
            field.set(Log, null)

            assertThatCode {
                Log.error("error")
                Log.warn("warn")
                Log.info("info")
                Log.debug("debug")
            }.doesNotThrowAnyException()
        }

        it("delegates to gradle logger") {
            val logger = mock<Logger>()
            Log.setLogger(logger)

            Log.error("error msg")
            Log.warn("warn msg")
            Log.info("info msg")
            Log.debug("debug msg")

            verify(logger).error(eq("error msg"), isNull<Throwable>())
            verify(logger).warn(eq("warn msg"), isNull<Throwable>())
            verify(logger).info("info msg")
            verify(logger).debug("debug msg")
        }

        it("passes throwable to error and warn") {
            val logger = mock<Logger>()
            Log.setLogger(logger)
            val exception = RuntimeException("test")

            Log.error("err", exception)
            Log.warn("wrn", exception)

            verify(logger).error(eq("err"), eq(exception))
            verify(logger).warn(eq("wrn"), eq(exception))
        }
    }
})
