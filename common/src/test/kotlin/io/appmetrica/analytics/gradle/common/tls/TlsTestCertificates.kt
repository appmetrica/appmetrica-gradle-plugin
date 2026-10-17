package io.appmetrica.analytics.gradle.common.tls

import java.io.InputStream
import java.security.cert.CertificateFactory
import java.security.cert.PKIXBuilderParameters
import java.security.cert.TrustAnchor
import java.security.cert.X509CertSelector
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.Date
import javax.net.ssl.CertPathTrustManagerParameters
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

internal object TlsTestCertificates {

    fun open(resourceName: String): InputStream =
        requireNotNull(TlsTestCertificates::class.java.getResourceAsStream(resourceName)) {
            "Missing test resource $resourceName"
        }

    fun load(resourceName: String): X509Certificate {
        return open(resourceName).use { stream ->
            CertificateFactory.getInstance("X.509").generateCertificate(stream) as X509Certificate
        }
    }

    fun trustManagerAt(
        root: X509Certificate,
        validationTime: Instant,
    ): X509TrustManager {
        val parameters = PKIXBuilderParameters(
            setOf(TrustAnchor(root, null)),
            X509CertSelector(),
        ).apply {
            date = Date.from(validationTime)
            // Fixture validation must not depend on network revocation services.
            isRevocationEnabled = false
        }

        return TrustManagerFactory.getInstance("PKIX").apply {
            init(CertPathTrustManagerParameters(parameters))
        }.trustManagers.filterIsInstance<X509TrustManager>().first()
    }
}
