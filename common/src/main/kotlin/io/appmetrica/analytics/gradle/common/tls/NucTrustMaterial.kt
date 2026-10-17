package io.appmetrica.analytics.gradle.common.tls

import io.appmetrica.analytics.gradle.common.utils.Log
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Loads the embedded Russian Trusted Root CA (`nuc_cert.crt`) into a KeyStore-backed
 * [X509TrustManager]. Certificate Transparency is not applied.
 */
internal object NucTrustMaterial {

    const val RESOURCE_NAME = "nuc_cert.crt"
    private const val CERTIFICATE_ALIAS = "nuc_cert"

    fun openEmbeddedNucCertStream(): InputStream? =
        NucTrustMaterial::class.java.getResourceAsStream(RESOURCE_NAME)

    @Suppress("TooGenericExceptionCaught")
    fun createTrustManager(inputStream: InputStream?): X509TrustManager? {
        if (inputStream == null) {
            Log.warn("Embedded NUC root resource is missing; using system trust only")
            return null
        }
        return try {
            inputStream.use { stream ->
                val certificate = CertificateFactory.getInstance("X.509")
                    .generateCertificate(stream) as X509Certificate
                trustManagerForCertificate(certificate)
            }
        } catch (throwable: Throwable) {
            Log.warn("Failed to load embedded NUC root; using system trust only", throwable)
            null
        }
    }

    fun trustManagerForCertificate(certificate: X509Certificate): X509TrustManager? {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            setCertificateEntry(CERTIFICATE_ALIAS, certificate)
        }
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(keyStore)
        return factory.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
    }
}
