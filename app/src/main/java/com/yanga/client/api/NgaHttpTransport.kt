package com.yanga.client.api

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

interface NgaHttpTransport {
  fun execute(request: NgaRequest): Result<NgaHttpResponse>
}

class HttpUrlConnectionNgaTransport(
  private val connectTimeoutMillis: Int = 15_000,
  private val readTimeoutMillis: Int = 15_000,
) : NgaHttpTransport {
  override fun execute(request: NgaRequest): Result<NgaHttpResponse> = runCatching {
    val connection = (URL(request.fullUrl()).openConnection() as HttpURLConnection).apply {
      requestMethod = request.method.name
      connectTimeout = connectTimeoutMillis
      readTimeout = readTimeoutMillis
      request.headers.forEach { (key, value) -> setRequestProperty(key, value) }
    }

    if (request.method == NgaHttpMethod.POST) {
      connection.doOutput = true
      val body = request.body.fields.joinToString("&")
      if (body.isNotEmpty()) {
        connection.outputStream.use { output ->
          output.write(body.toByteArray(Charset.forName("GBK")))
        }
      }
    }

    val code = connection.responseCode
    val contentType = connection.contentType
    val charset = if (contentType?.contains("charset=utf-8", ignoreCase = true) == true) {
      Charsets.UTF_8
    } else if (contentType?.contains("charset=gbk", ignoreCase = true) == true) {
      Charset.forName("GBK")
    } else {
      // Default to GBK for NGA if not specified, but check URL
      if (request.url.contains("app_api.php")) Charsets.UTF_8 else Charset.forName("GBK")
    }

    val stream = if (code in 200..399) connection.inputStream else connection.errorStream
    val text = stream?.use {
      BufferedReader(InputStreamReader(it, charset)).readText()
    }.orEmpty()
    NgaHttpResponse(code = code, text = text)
  }
}
