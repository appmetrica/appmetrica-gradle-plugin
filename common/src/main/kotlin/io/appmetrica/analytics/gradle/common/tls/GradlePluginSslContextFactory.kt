package io.appmetrica.analytics.gradle.common.tls

import io.appmetrica.analytics.gradle.common.utils.Log
import java.io.InputStream
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Provides an [SSLContext] for AppMetrica Gradle Plugin HTTPS uploads:
 * system trust store first, then the embedded NUC root. No Certificate Transparency.
 *
 * Init failure never yields a trust-all context: falls back to [SSLContext.getDefault].
 */
object GradlePluginSslContextFactory {

    fun sslContext(): SSLContext = createSslContext()

    /**
     * Builds a trust manager: system-first, then NUC when the resource loads successfully.
     * Visible for tests (custom resource stream).
     */
    internal fun createTrustManager(
        nucCertStream: () -> InputStream? = { NucTrustMaterial.openEmbeddedNucCertStream() },
    ): X509TrustManager {
        val systemTrustManager = requireNotNull(defaultTrustManager()) {
            "System X509TrustManager is unavailable"
        }
        val nucTrustManager = NucTrustMaterial.createTrustManager(nucCertStream())
        return if (nucTrustManager != null) {
            CompositeTrustManager(listOf(systemTrustManager, nucTrustManager))
        } else {
            systemTrustManager
        }
    }

    @Suppress("TooGenericExceptionCaught")
    internal fun createSslContext(
        nucCertStream: () -> InputStream? = { NucTrustMaterial.openEmbeddedNucCertStream() },
    ): SSLContext {
        return try {
            val trustManager = createTrustManager(nucCertStream)
            SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(trustManager), null)
            }
        } catch (throwable: Throwable) {
            Log.warn(
                "Failed to initialize Gradle plugin SSLContext; falling back to SSLContext.getDefault()",
                throwable
            )
            SSLContext.getDefault()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun defaultTrustManager(): X509TrustManager? {
        return try {
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            factory.init(null as KeyStore?)
            factory.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
        } catch (throwable: Throwable) {
            Log.warn("Failed to create system trust manager", throwable)
            null
        }
    }
}
