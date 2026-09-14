package com.yanga.client.data.image

import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Persists avatar and post images as original downloaded bytes (JPEG/PNG/WebP/etc.).
 */
internal class ContentImageRawCache(
  private val cacheDir: File,
  indexFile: File,
  private val userAgent: String = DEFAULT_USER_AGENT,
) {
  private val index = ImageCacheIndex(indexFile)

  fun get(rawUrl: String): File? {
    val url = ImageUrlResolver.resolveForRequest(rawUrl)
    if (url.isBlank()) return null
    index.getByUrl(url)?.let { path ->
      val file = File(path)
      if (file.exists()) return file
    }
    return null
  }

  fun has(rawUrl: String): Boolean = get(rawUrl) != null

  fun store(rawUrl: String, bytes: ByteArray): File? {
    val url = ImageUrlResolver.resolveForRequest(rawUrl)
    if (url.isBlank() || bytes.isEmpty()) return null
    val file = fileFor(url)
    file.parentFile?.mkdirs()
    file.writeBytes(bytes)
    index.put(url, file.absolutePath)
    return file
  }

  fun download(rawUrl: String): File? {
    val url = ImageUrlResolver.resolveForRequest(rawUrl)
    if (url.isBlank()) return null
    get(url)?.let { return it }
    return runCatching {
      val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = CONNECT_TIMEOUT_MS
        readTimeout = READ_TIMEOUT_MS
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", userAgent)
        if (URL(url).host.endsWith(".nga.cn")) setRequestProperty("Referer", "https://bbs.nga.cn/")
      }
      try {
        if (connection.responseCode !in SUCCESS_STATUS_RANGE) {
          Log.w(LOG_TAG, "download failed code=${connection.responseCode} url=$url")
          return null
        }
        connection.inputStream.use { input ->
          store(url, input.readBytes())
        }
      } finally {
        connection.disconnect()
      }
    }.onFailure { error ->
      Log.w(LOG_TAG, "download failed url=$url", error)
    }.getOrNull()
  }

  private fun fileFor(url: String): File {
    cacheDir.mkdirs()
    return cacheDir.resolve("${urlHash(url)}.${extensionFor(url)}")
  }

  private fun urlHash(url: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(url.toByteArray(Charsets.UTF_8))
      .joinToString("") { byte -> "%02x".format(byte) }
  }

  private fun extensionFor(url: String): String {
    val name = ImageUrlResolver.fileName(url)
    val ext = name.substringAfterLast('.', "")
    return if (ext.length in 2..5 && ext.all { it.isLetterOrDigit() }) {
      ext.lowercase()
    } else {
      DEFAULT_EXTENSION
    }
  }

  private companion object {
    const val CONNECT_TIMEOUT_MS = 15_000
    const val READ_TIMEOUT_MS = 30_000
    const val DEFAULT_EXTENSION = "jpg"
    const val DEFAULT_USER_AGENT = "Yanga Android"
    const val LOG_TAG = "YangaImageCache"
    val SUCCESS_STATUS_RANGE = 200..299
  }
}
