package com.yanga.client.ui.navigation

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class ForumThreadDestination(
  val tid: String,
  val page: Int = 1,
)

object NgaForumLinkParser {
  private val forumHosts =
    setOf(
      "bbs.nga.cn",
      "nga.178.com",
      "ngabbs.com",
      "bbs.ngacn.cc",
    )

  fun threadDestination(rawUrl: String?): ForumThreadDestination? {
    if (rawUrl.isNullOrBlank()) return null
    val uri = runCatching { URI(rawUrl.trim()) }.getOrNull() ?: return null

    if (uri.scheme.equals("nga", ignoreCase = true) && uri.host.equals("thread", ignoreCase = true)) {
      val tid = uri.path.trim('/').takeIf { it.all(Char::isDigit) } ?: return null
      return ForumThreadDestination(tid = tid)
    }

    val host = uri.host?.lowercase() ?: return null
    if (host !in forumHosts) return null
    if (!uri.path.orEmpty().endsWith("/read.php", ignoreCase = true)) return null

    val params = parseQuery(uri.rawQuery)
    val tid = params["tid"]?.takeIf { it.all(Char::isDigit) } ?: return null
    val page = params["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    return ForumThreadDestination(tid = tid, page = page)
  }

  fun isForumUrl(rawUrl: String?): Boolean {
    if (rawUrl.isNullOrBlank()) return false
    val uri = runCatching { URI(rawUrl.trim()) }.getOrNull() ?: return false
    if (uri.scheme.equals("nga", ignoreCase = true)) return true
    return uri.host?.lowercase() in forumHosts
  }

  private fun parseQuery(rawQuery: String?): Map<String, String> {
    if (rawQuery.isNullOrBlank()) return emptyMap()
    return rawQuery
      .split('&')
      .mapNotNull { pair ->
        val parts = pair.split('=', limit = 2)
        val key = parts.getOrNull(0)?.decodeQueryPart()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val value = parts.getOrNull(1)?.decodeQueryPart().orEmpty()
        key to value
      }
      .toMap()
  }

  private fun String.decodeQueryPart(): String =
    URLDecoder.decode(this, StandardCharsets.UTF_8.name())
}
