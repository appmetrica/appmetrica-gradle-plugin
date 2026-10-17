package io.appmetrica.analytics.gradle.common.tls

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

object CompositeTrustManagerSpek : Spek({
    describe("CompositeTrustManager") {
        val chain = arrayOf(mock<X509Certificate>())
        val firstIssuer = mock<X509Certificate>()
        val secondIssuer = mock<X509Certificate>()

        it("accepts first successful delegate, falls back, aggregates issuers; rejects empty") {
            assertThatThrownBy { CompositeTrustManager(emptyList()) }
                .isInstanceOf(IllegalArgumentException::class.java)

            val first: X509TrustManager = mock {
                on { checkServerTrusted(anyOrNull(), anyOrNull()) } doThrow CertificateException("system")
                on { checkClientTrusted(anyOrNull(), anyOrNull()) } doThrow CertificateException("system")
                on { acceptedIssuers } doReturn arrayOf(firstIssuer)
            }
            val second: X509TrustManager = mock {
                on { acceptedIssuers } doReturn arrayOf(secondIssuer)
            }
            val underTest = CompositeTrustManager(listOf(first, second))

            underTest.checkServerTrusted(chain, "RSA")
            underTest.checkClientTrusted(chain, "RSA")
            verify(first).checkServerTrusted(chain, "RSA")
            verify(second).checkServerTrusted(chain, "RSA")
            verify(first).checkClientTrusted(chain, "RSA")
            verify(second).checkClientTrusted(chain, "RSA")
            assertThat(underTest.acceptedIssuers).containsExactly(firstIssuer, secondIssuer)

            val unused: X509TrustManager = mock()
            val acceptedFirst: X509TrustManager = mock {
                on { acceptedIssuers } doReturn emptyArray()
            }
            CompositeTrustManager(listOf(acceptedFirst, unused)).checkServerTrusted(chain, "RSA")
            verify(unused, never()).checkServerTrusted(chain, "RSA")

            val rejectingFirst: X509TrustManager = mock {
                on { checkServerTrusted(anyOrNull(), anyOrNull()) } doThrow CertificateException("first")
            }
            val rejectingSecond: X509TrustManager = mock {
                on { checkServerTrusted(anyOrNull(), anyOrNull()) } doThrow CertificateException("second")
            }
            assertThatThrownBy {
                CompositeTrustManager(listOf(rejectingFirst, rejectingSecond)).checkServerTrusted(chain, "RSA")
            }.isInstanceOf(CertificateException::class.java).hasMessage("second")
        }
    }
})
