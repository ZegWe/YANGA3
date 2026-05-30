package com.yanga.client.data.image

import org.json.JSONObject
import java.io.File

internal class ImageCacheIndex(
  private val indexFile: File,
) {
  private val lock = Any()
  private var byUrl = mutableMapOf<String, String>()
  private var byFileName = mutableMapOf<String, String>()

  init {
    load()
  }

  fun put(url: String, localPath: String) {
    if (url.isBlank() || localPath.isBlank()) return
    synchronized(lock) {
      byUrl[url] = localPath
      val fileName = ImageUrlResolver.fileName(url)
      if (fileName.isNotBlank()) {
        byFileName[fileName] = localPath
      }
      persistLocked()
    }
  }

  fun getByUrl(url: String): String? =
    synchronized(lock) {
      byUrl[url]
    }

  fun getByFileName(fileName: String): String? =
    synchronized(lock) {
      byFileName[fileName]
    }

  private fun load() {
    if (!indexFile.exists()) return
    runCatching {
      val root = JSONObject(indexFile.readText())
      byUrl = root.optJSONObject(KEY_BY_URL).toStringMap().toMutableMap()
      byFileName = root.optJSONObject(KEY_BY_FILE_NAME).toStringMap().toMutableMap()
    }
  }

  private fun persistLocked() {
    indexFile.parentFile?.mkdirs()
    val payload =
      JSONObject()
        .put(KEY_BY_URL, byUrl.toJsonObject())
        .put(KEY_BY_FILE_NAME, byFileName.toJsonObject())
    indexFile.writeText(payload.toString())
  }

  private fun Map<String, String>.toJsonObject(): JSONObject {
    val json = JSONObject()
    forEach { (key, value) -> json.put(key, value) }
    return json
  }

  private fun JSONObject?.toStringMap(): Map<String, String> {
    if (this == null) return emptyMap()
    return keys().asSequence().associateWith { key -> optString(key) }.filterValues { it.isNotBlank() }
  }

  private companion object {
    const val KEY_BY_URL = "byUrl"
    const val KEY_BY_FILE_NAME = "byFileName"
  }
}
