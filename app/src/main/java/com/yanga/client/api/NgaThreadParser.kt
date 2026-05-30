package com.yanga.client.api

import org.json.JSONObject

data class NgaThreadRead(
  val tid: String,
  val subject: String,
  val fid: String,
  val page: Int,
  val posts: List<NgaThreadPost>,
)

data class NgaThreadPost(
  val pid: String,
  val tid: String,
  val fid: String,
  val authorId: String,
  val author: String,
  val authorAvatarUrl: String? = null,
  val subject: String,
  val content: String,
  val lou: Int,
  val postDate: Long,
)

object NgaThreadParser {
  fun parseRead(raw: String): NgaThreadRead {
    val root = JSONObject(NgaResponseNormalizer.normalize(raw))
    val data = root.optJSONObject("data") ?: JSONObject()
    val topic = data.optJSONObject("__T") ?: JSONObject()
    val replies = data.optJSONObject("__R") ?: JSONObject()
    val users = data.optJSONObject("__U") ?: JSONObject()

    val posts = replies.keys().asSequence()
      .sortedWith(compareBy { it.toIntOrNull() ?: Int.MAX_VALUE })
      .mapNotNull { key -> replies.optJSONObject(key)?.toPost(topic, users) }
      .toList()

    return NgaThreadRead(
      tid = topic.stringValue("tid").ifBlank { posts.firstOrNull()?.tid.orEmpty() },
      subject = topic.stringValue("subject").ifBlank { posts.firstOrNull()?.subject.orEmpty() },
      fid = topic.stringValue("fid").ifBlank { posts.firstOrNull()?.fid.orEmpty() },
      page = data.intValue("__PAGE"),
      posts = posts,
    )
  }

  private fun JSONObject.toPost(topic: JSONObject, users: JSONObject): NgaThreadPost {
    val authorId = stringValue("authorid")
    val user = users.optJSONObject(authorId)
    val author = resolveAuthorName(user)
    val avatarRaw = user?.nullableStringValue("avatar") ?: nullableStringValue("avatar")
    return NgaThreadPost(
      pid = stringValue("pid"),
      tid = stringValue("tid").ifBlank { topic.stringValue("tid") },
      fid = stringValue("fid").ifBlank { topic.stringValue("fid") },
      authorId = authorId,
      author = author,
      authorAvatarUrl = NgaAvatarUrls.resolve(avatarRaw, authorId),
      subject = stringValue("subject").ifBlank { topic.stringValue("subject") },
      content = stringValue("content"),
      lou = intValue("lou"),
      postDate = longValue("postdatetimestamp").takeIf { it > 0 } ?: longValue("postdate"),
    )
  }

  private fun JSONObject.resolveAuthorName(user: JSONObject?): String =
    stringValue("author")
      .ifBlank { user?.stringValue("username").orEmpty() }
      .ifBlank { user?.nullableStringValue("nickname").orEmpty() }

  private fun JSONObject.nullableStringValue(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key ->
      opt(key)?.takeUnless { it == JSONObject.NULL }?.toString()?.takeIf { it.isNotBlank() }
    }

  private fun JSONObject.stringValue(key: String): String =
    opt(key)?.takeUnless { it == JSONObject.NULL }?.toString().orEmpty()

  private fun JSONObject.intValue(key: String): Int =
    when (val value = opt(key)) {
      is Number -> value.toInt()
      is String -> value.toIntOrNull() ?: 0
      else -> 0
    }

  private fun JSONObject.longValue(key: String): Long =
    when (val value = opt(key)) {
      is Number -> value.toLong()
      is String -> value.toLongOrNull() ?: 0L
      else -> 0L
    }
}
