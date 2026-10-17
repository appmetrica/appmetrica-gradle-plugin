package io.appmetrica.analytics.gradle.common.tls

import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * [X509TrustManager] that delegates server/client checks to an ordered list of trust managers.
 *
 * The first delegate that accepts the chain wins; [CertificateException] falls through to the
 * next. The chain is rejected only if every delegate rejects it (last error is rethrown).
 *
 * Order: put the system trust manager first and the NUC-only manager second so public CA chains
 * succeed without the domestic root path.
 *
 * Adapted from Yandex Browser LibSSL composite trust-manager logic (without CT wrap / Android APIs).
 */
internal class CompositeTrustManager(
    private val delegates: List<X509TrustManager>,
) : X509TrustManager {

    init {
        require(delegates.isNotEmpty()) { "delegates must not be empty" }
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        checkTrusted("No trust managers to check server certificate") {
            it.checkServerTrusted(chain, authType)
        }
    }

    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        checkTrusted("No trust managers to check client certificate") {
            it.checkClientTrusted(chain, authType)
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> =
        delegates.flatMap { it.acceptedIssuers.asList() }.toTypedArray()

    @Throws(CertificateException::class)
    private inline fun checkTrusted(
        noDelegatesMessage: String,
        check: (X509TrustManager) -> Unit,
    ) {
        var lastError: CertificateException? = null
        for (delegate in delegates) {
            try {
                check(delegate)
                return
            } catch (e: CertificateException) {
                lastError = e
            }
        }
        throw lastError ?: CertificateException(noDelegatesMessage)
    }
}
