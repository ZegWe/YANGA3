package com.yanga.client.api

import com.yanga.client.diagnostics.Diagnostics
import com.yanga.client.diagnostics.codeName
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
    val started = System.nanoTime()
    val route = when (request.url.substringBefore('?').substringAfterLast('/')) {
      "read.php" -> if (request.query.containsKey("__output")) "thread_json" else "thread_html"
      "thread.php" -> "board_topics"
      "app_api.php" -> "app_api"
      else -> "other"
    }
    val connection = URL(request.fullUrl()).openConnection() as HttpURLConnection
    try {
      connection.apply {
        requestMethod = request.method.name
        connectTimeout = connectTimeoutMillis
        readTimeout = readTimeoutMillis
        request.headers.forEach { (key, value) -> setRequestProperty(key, value) }
      }

      if (request.method == NgaHttpMethod.POST) {
        connection.doOutput = true
        val body = request.binaryBody ?: request.body.fields.joinToString("&").toByteArray(Charset.forName("GBK"))
        if (body.isNotEmpty()) {
          connection.setFixedLengthStreamingMode(body.size)
          connection.outputStream.use { output ->
            output.write(body)
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
      Diagnostics.event {
        val supplied = contentType?.let { Regex("charset\\s*=\\s*[^a-zA-Z0-9_-]*([a-zA-Z0-9_-]+)", RegexOption.IGNORE_CASE)
          .find(it)?.groupValues?.get(1)?.lowercase() }
        val declared = supplied?.takeIf { it in setOf("utf-8", "utf8", "gbk", "gb2312", "gb18030", "big5", "iso-8859-1", "windows-1252", "us-ascii") }
          ?: if (supplied == null) "missing" else "other"
        val thread = request.query["tid"]?.takeIf { route == "thread_json" || route == "thread_html" }
          ?.let(Diagnostics::fingerprint) ?: "none"
        val page = request.query["page"]?.toIntOrNull() ?: 0
        "http_done route=$route tid=$thread page=$page method=${request.method.name} status=$code " +
          "declared=$declared used=${charset.name()} ms=${(System.nanoTime() - started) / 1_000_000} " +
          if (route == "thread_json" || route == "board_topics") Diagnostics.textStats(text) else "chars=${text.length}"
      }
      NgaHttpResponse(code = code, text = text)
    } catch (error: Exception) {
      Diagnostics.event { "http_failed route=$route type=${codeName(error.javaClass.name)} ms=${(System.nanoTime() - started) / 1_000_000}" }
      throw error
    } finally {
      connection.disconnect()
    }
  }
}
