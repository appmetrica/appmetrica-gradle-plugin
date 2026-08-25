package io.appmetrica.analytics.gradle.common

import io.appmetrica.analytics.gradle.common.utils.Log
import java.io.File
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Suppress("MagicNumber")
class FileUploader(
    private val url: String,
    private val postApiKey: String
) {

    fun uploadFile(zippedFile: File) {
        val httpClientBuilder = HttpClient.newBuilder()
            .proxy(ProxySelector.getDefault())
        HttpSystemTimeouts.connectTimeout()?.let { httpClientBuilder.connectTimeout(it) }

        val request = HttpRequest.newBuilder(URI.create(url))
            .PUT(HttpRequest.BodyPublishers.ofFile(zippedFile.toPath()))
            .header("Authorization", "Post-Api-Key $postApiKey")
            .header("Content-Type", "application/zip; charset=utf-8")
            .build()

        val httpClient = httpClientBuilder.build()

        Log.info("Executing request PUT $url")

        var lastServerError: HttpResponseException? = null
        repeat(MAX_RETRY_COUNT) { attempt ->
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val status = response.statusCode()
            val body = response.body().orEmpty()
            when {
                status in 200..299 -> {
                    Log.info("Request succeeded with response body $body")
                    return
                }
                status in 500..599 -> {
                    lastServerError = HttpResponseException(
                        status,
                        "Unexpected response status $status $body"
                    )
                    if (attempt < MAX_RETRY_COUNT - 1) {
                        Log.debug("Request failed with status code $status. Retrying...")
                    }
                }
                else -> {
                    throw HttpResponseException(status, "Unexpected response status $status $body")
                }
            }
        }
        throw requireNotNull(lastServerError) {
            "Upload failed after $MAX_RETRY_COUNT attempts without a captured server error"
        }
    }
}
