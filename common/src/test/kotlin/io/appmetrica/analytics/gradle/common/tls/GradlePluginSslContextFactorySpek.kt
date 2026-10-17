package io.appmetrica.analytics.gradle.common.tls

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.ByteArrayInputStream
import java.security.cert.CertificateException
import java.time.Instant
import javax.net.ssl.X509TrustManager

object GradlePluginSslContextFactorySpek : Spek({
    describe("GradlePluginSslContextFactory trust path") {
        it("builds usable SSLContext and accepts system / NUC chains; rejects alternate / broken") {
            val sslContext = GradlePluginSslContextFactory.sslContext()
            assertThat(sslContext).isNotNull
            assertThat(sslContext.socketFactory).isNotNull

            val validationTime = Instant.parse("2026-10-13T12:00:00Z")

            val pluginTrustManager = GradlePluginSslContextFactory.createTrustManager()
            val nucRoot = TlsTestCertificates.load("nuc_cert.crt")

            assertThat(pluginTrustManager)
                .isInstanceOf(CompositeTrustManager::class.java)
            assertThat(pluginTrustManager.acceptedIssuers).contains(nucRoot)

            val systemRoot = TlsTestCertificates.load("system_root.pem")
            val systemLeaf = TlsTestCertificates.load("system_leaf.pem")

            val systemFirstComposite = CompositeTrustManager(
                listOf(
                    TlsTestCertificates.trustManagerAt(systemRoot, validationTime),
                    TlsTestCertificates.trustManagerAt(nucRoot, validationTime),
                )
            )

            systemFirstComposite.checkServerTrusted(
                arrayOf(systemLeaf),
                "ECDHE_ECDSA",
            )

            val nucLeaf = TlsTestCertificates.load("nuc_chain_leaf.pem")
            val nucIntermediate = TlsTestCertificates.load("nuc_chain_intermediate.pem")
            val nucChain = arrayOf(nucLeaf, nucIntermediate)

            systemFirstComposite.checkServerTrusted(nucChain, "RSA")

            val alternateRoot = TlsTestCertificates.load("alternate_root.pem")
            val alternateLeaf = TlsTestCertificates.load("alternate_leaf.pem")
            val alternateChain = arrayOf(alternateLeaf)

            val alternateOnly = TlsTestCertificates.trustManagerAt(
                alternateRoot,
                validationTime,
            )
            alternateOnly.checkServerTrusted(alternateChain, "ECDHE_ECDSA")

            assertThatThrownBy {
                systemFirstComposite.checkServerTrusted(
                    alternateChain,
                    "ECDHE_ECDSA",
                )
            }.isInstanceOf(CertificateException::class.java)

            val embeddedNucTrustManager = requireNotNull(
                NucTrustMaterial.createTrustManager(
                    NucTrustMaterial.openEmbeddedNucCertStream()
                )
            )
            val tciRoot = TlsTestCertificates.load("tci_ecdsa_root_a1.crt")

            assertThat(
                embeddedNucTrustManager.acceptedIssuers.map {
                    it.subjectX500Principal
                }
            ).doesNotContain(tciRoot.subjectX500Principal)

            val afterExpiry = nucLeaf.notAfter.toInstant().plusSeconds(1)
            val expiredComposite = CompositeTrustManager(
                listOf(
                    TlsTestCertificates.trustManagerAt(systemRoot, afterExpiry),
                    TlsTestCertificates.trustManagerAt(nucRoot, afterExpiry),
                )
            )

            assertThatThrownBy {
                expiredComposite.checkServerTrusted(nucChain, "RSA")
            }.isInstanceOf(CertificateException::class.java)

            assertThatThrownBy {
                systemFirstComposite.checkServerTrusted(
                    arrayOf(TlsTestCertificates.load("self_signed.pem")),
                    "ECDHE_ECDSA",
                )
            }.isInstanceOf(CertificateException::class.java)
        }

        it("falls back to system-only trust when NUC resource is missing or corrupt") {
            val missing = GradlePluginSslContextFactory.createTrustManager { null }
            assertThat(missing).isNotInstanceOf(CompositeTrustManager::class.java)
            assertRejectsAlternate(missing)

            val corrupt = GradlePluginSslContextFactory.createTrustManager {
                TlsTestCertificates.open("corrupt_nuc_cert.crt")
            }
            assertThat(corrupt).isNotInstanceOf(CompositeTrustManager::class.java)
            assertRejectsAlternate(corrupt)

            val emptyBytes = GradlePluginSslContextFactory.createSslContext {
                ByteArrayInputStream(ByteArray(0))
            }
            assertThat(emptyBytes).isNotNull
            assertThat(emptyBytes.socketFactory).isNotNull
        }
    }
})

private fun assertRejectsAlternate(trustManager: X509TrustManager) {
    assertThatThrownBy {
        trustManager.checkServerTrusted(
            arrayOf(TlsTestCertificates.load("alternate_leaf.pem")),
            "ECDHE_ECDSA"
        )
    }.isInstanceOf(CertificateException::class.java)
}
