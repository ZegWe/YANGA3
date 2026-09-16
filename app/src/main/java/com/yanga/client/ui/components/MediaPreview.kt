package com.yanga.client.ui.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import com.yanga.client.api.NgaDisplayText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

internal data class MediaPreview(val title: String? = null, val cover: String? = null, val frame: Bitmap? = null)

internal fun webMediaUrl(base: String, value: String): String? = runCatching {
  URI(base).resolve(NgaDisplayText.decodeEntities(value.trim())).let {
    if (it.scheme in listOf("https", "http") && !it.host.isNullOrBlank()) it.toString() else null
  }
}.getOrNull()

internal fun parseMediaMetadata(url: String, html: String): Pair<String?, String?> {
  val values = mutableMapOf<String, String>()
  Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(html).forEach { tag ->
    val attrs = Regex("([\\w:-]+)\\s*=\\s*([\"'])(.*?)\\2", RegexOption.DOT_MATCHES_ALL)
      .findAll(tag.value).associate { it.groupValues[1].lowercase() to it.groupValues[3] }
    val key = attrs["property"] ?: attrs["name"]
    if (key != null) attrs["content"]?.let { values.putIfAbsent(key.lowercase(), it) }
  }
  val title = values["og:title"] ?: values["twitter:title"] ?: Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)?.groupValues?.get(1)
  val cover = values["og:image"] ?: values["twitter:image"] ?: values["twitter:image:src"]
  return title?.let { NgaDisplayText.decodeEntities(it).trim().take(250) } to cover?.let { webMediaUrl(url, it) }
}

internal object MediaPreviewLoader {
  private val requests = Semaphore(2)
  private val cache = object : LruCache<String, MediaPreview>(12 * 1024 * 1024) {
    override fun sizeOf(key: String, value: MediaPreview) = value.frame?.byteCount ?: 1024
  }

  suspend fun load(url: String, direct: Boolean): MediaPreview = withContext(Dispatchers.IO) {
    cache.get(url) ?: requests.withPermit {
      cache.get(url) ?: runCatching {
        if (direct) thumbnail(url) else metadata(url)
      }.getOrDefault(MediaPreview()).also { cache.put(url, it) }
    }
  }

  private fun thumbnail(url: String): MediaPreview {
    val retriever = MediaMetadataRetriever()
    return try {
      if (url.startsWith("file:")) retriever.setDataSource(URI(url).path)
      else retriever.setDataSource(url, mapOf("User-Agent" to "Yanga Android", "Referer" to "https://bbs.nga.cn/"))
      val frame = if (Build.VERSION.SDK_INT >= 27) retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 640, 360)
      else retriever.getFrameAtTime(0)?.let { original ->
        val scale = minOf(640f / original.width, 360f / original.height, 1f)
        Bitmap.createScaledBitmap(original, (original.width * scale).toInt().coerceAtLeast(1), (original.height * scale).toInt().coerceAtLeast(1), true).also {
          if (it !== original) original.recycle()
        }
      }
      MediaPreview(frame = frame)
    } finally { retriever.release() }
  }

  private fun metadata(url: String): MediaPreview {
    val uri = URI(url)
    if (uri.host == "bilibili.com" || uri.host?.endsWith(".bilibili.com") == true) {
      val bvid = Regex("BV[a-zA-Z0-9]{10}").find(url)?.value
      if (bvid != null) {
        val data = runCatching { JSONObject(fetch("https://api.bilibili.com/x/web-interface/view?bvid=$bvid").second).optJSONObject("data") }.getOrNull()
        if (data != null) return MediaPreview(data.optString("title").takeIf { it.isNotBlank() },
          webMediaUrl(url, data.optString("pic").replaceFirst(Regex("^http://"), "https://")))
      }
    }
    val (finalUrl, html) = fetch(url)
    val (title, cover) = parseMediaMetadata(finalUrl, html)
    return MediaPreview(title, cover)
  }

  private fun fetch(initial: String): Pair<String, String> {
    var url = initial
    repeat(4) {
      require(webMediaUrl(url, url) != null)
      val connection = URI(url).toURL().openConnection() as HttpURLConnection
      try {
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 Yanga")
        val status = connection.responseCode
        if (status in 300..399) {
          url = webMediaUrl(url, connection.getHeaderField("Location") ?: error("Missing redirect")) ?: error("Invalid redirect")
        } else {
          require(status in 200..299)
          return url to connection.inputStream.use { it.readBytesLimited(512 * 1024).toString(Charsets.UTF_8) }
        }
      } finally { connection.disconnect() }
    }
    error("Too many redirects")
  }

  private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (output.size() < limit) {
      val count = read(buffer, 0, minOf(buffer.size, limit - output.size()))
      if (count < 0) break
      output.write(buffer, 0, count)
    }
    return output.toByteArray()
  }
}
