package io.appmetrica.analytics.gradle.common

import org.apache.http.client.HttpResponseException
import org.apache.http.client.ResponseHandler
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.FileEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.io.File

@Suppress("MagicNumber")
class FileUploader(
    private val url: String,
    private val postApiKey: String
) {

    private var retryCount = 0

    fun uploadFile(zippedFile: File) {
        val httpClient = HttpClients.custom()
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .build()
            )
            .build()
        val httpPut = HttpPut(url)
        httpPut.entity = FileEntity(zippedFile)
        httpPut.addHeader("Authorization", "Post-Api-Key $postApiKey")
        httpPut.addHeader("Content-Type", "application/zip; charset=utf-8")

        Log.info("Executing request ${httpPut.requestLine}")

        val responseHandler = ResponseHandler<String> { response ->
            val status = response.statusLine.statusCode
            val body = EntityUtils.toString(response.entity)
            if (status in 200..299) {
                body
            } else {
                throw HttpResponseException(status, "Unexpected response status $status $body")
            }
        }
        do {
            try {
                val responseBody = httpClient.execute(httpPut, responseHandler)
                Log.info("Request succeeded with response body $responseBody")
                return
            } catch (e: HttpResponseException) {
                if (e.statusCode in 500..599) {
                    Log.debug("Request failed with status code ${e.statusCode}. Retrying...")
                    retryCount += 1
                } else {
                    throw e
                }
            }
        } while (retryCount < MAX_RETRY_COUNT)
    }
}
