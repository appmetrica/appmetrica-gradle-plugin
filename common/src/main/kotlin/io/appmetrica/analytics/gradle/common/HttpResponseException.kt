package io.appmetrica.analytics.gradle.common

import java.io.IOException

class HttpResponseException(
    val statusCode: Int,
    message: String
) : IOException(message)
