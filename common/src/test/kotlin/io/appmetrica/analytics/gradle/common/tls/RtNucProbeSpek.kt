package io.appmetrica.analytics.gradle.common.tls

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.spekframework.spek2.Spek
import org.spekframework.spek2.dsl.Skip
import org.spekframework.spek2.style.specification.describe
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException

/**
 * Manual live probe for METRIKALIB-11042 against `https://rt-nuc.sec.yandex.net`.
 *
 * Skipped in CI: a clean JVM without the NUC root in the system truststore is required,
 * and the host is not a unit-test fixture. Enable locally:
 * `APPMETRICA_RT_NUC_PROBE=1 ./gradlew :common:test --tests='*RtNucProbeSpek'`
 */
object RtNucProbeSpek : Spek({
    describe("rt-nuc HTTPS probe") {
        it(
            "default JVM trust fails and plugin SSLContext succeeds",
            skip = rtNucProbeSkip(),
        ) {
            val uri = URI.create(RT_NUC_PROBE_URL)

            assertThatThrownBy { getStatus(uri, SSLContext.getDefault()) }
                .isInstanceOf(SSLException::class.java)

            val status = getStatus(uri, GradlePluginSslContextFactory.sslContext())
            assertThat(status).isGreaterThanOrEqualTo(200)
        }
    }
})

private const val RT_NUC_PROBE_URL = "https://rt-nuc.sec.yandex.net"
private const val RT_NUC_PROBE_ENV = "APPMETRICA_RT_NUC_PROBE"
private val RT_NUC_PROBE_TIMEOUT: Duration = Duration.ofSeconds(15)

private fun rtNucProbeSkip(): Skip {
    if (System.getenv(RT_NUC_PROBE_ENV) == "1") {
        return Skip.No
    }
    return Skip.Yes(
        "Live HTTPS to $RT_NUC_PROBE_URL is not run in CI. " +
            "Set $RT_NUC_PROBE_ENV=1 to run locally on a JVM without the NUC root in the system truststore."
    )
}

private fun getStatus(uri: URI, sslContext: SSLContext): Int {
    val client = HttpClient.newBuilder()
        .proxy(ProxySelector.getDefault())
        .sslContext(sslContext)
        .connectTimeout(RT_NUC_PROBE_TIMEOUT)
        .build()
    val request = HttpRequest.newBuilder(uri)
        .GET()
        .timeout(RT_NUC_PROBE_TIMEOUT)
        .build()
    return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
}
