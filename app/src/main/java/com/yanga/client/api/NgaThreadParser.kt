package com.yanga.client.api

import com.yanga.client.data.image.ImageUrlResolver
import org.json.JSONObject

data class NgaThreadRead(
  val tid: String,
  val subject: String,
  val fid: String,
  val page: Int,
  val replyCount: Int = 0,
  val maxPage: Int = 1,
  val posts: List<NgaThreadPost>,
)

data class NgaThreadEmbeddedReply(
  val pid: String,
  val tid: String,
  val authorId: String,
  val author: String,
  val authorAvatarUrl: String? = null,
  val content: String,
  val postDate: Long,
  val score: Int = 0,
  val lou: Int = 0,
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
  val editDate: Long? = null,
  val embeddedComments: List<NgaThreadEmbeddedReply> = emptyList(),
  val hotReplies: List<NgaThreadEmbeddedReply> = emptyList(),
  val attachments: List<NgaThreadAttachment> = emptyList(),
  val poll: NgaPoll? = null,
  val score: Int = 0,
  val isOriginalPoster: Boolean = false,
  val authorMemberId: Int = 0,
  val bodyColor: String? = null,
)

data class NgaThreadAttachment(
  val name: String,
  val url: String,
)

object NgaThreadParser {
  private fun resolveAttachmentContent(content: String, attachmentBase: String?): String =
    if (attachmentBase.isNullOrBlank()) content else
    Regex("""(\[(?:img|flash|media|audio|video)[^\]]*\]\s*)((?:\./|/?mon_)[^\s\[<>]+)""", RegexOption.IGNORE_CASE)
      .replace(content) { match ->
        match.groupValues[1] + ImageUrlResolver.resolve(match.groupValues[2], attachmentBase)
      }

  fun parseRead(raw: String): NgaThreadRead {
    val root = JSONObject(NgaResponseNormalizer.normalize(raw))
    val data = root.optJSONObject("data") ?: JSONObject()
    val topic = data.optJSONObject("__T") ?: JSONObject()
    val replies = data.optJSONObject("__R") ?: JSONObject()
    val users = data.optJSONObject("__U") ?: JSONObject()
    val attachmentBase = data.optJSONObject("__GLOBAL")?.optString("_ATTACH_BASE_VIEW")

    val commentContentByPid = buildCommentContentIndex(replies.optJSONObject("0"), topic, users, attachmentBase)

    val posts = replies.keys().asSequence()
      .sortedWith(compareBy { it.toIntOrNull() ?: Int.MAX_VALUE })
      .mapNotNull { key -> replies.optJSONObject(key)?.toPost(topic, users, attachmentBase, commentContentByPid) }
      .toList()

    val replyCount = topic.intValue("replies").coerceAtLeast(0)
    val rowCount = data.intValue("__ROWS").takeIf { it > 0 } ?: (replyCount + 1)
    return NgaThreadRead(
      tid = topic.stringValue("tid").ifBlank { posts.firstOrNull()?.tid.orEmpty() },
      subject = topic.stringValue("subject").takeIf { it.isNotBlank() }?.let(NgaDisplayText::singleLine)
        ?: posts.firstOrNull()?.subject.orEmpty(),
      fid = topic.stringValue("fid").ifBlank { posts.firstOrNull()?.fid.orEmpty() },
      page = data.intValue("__PAGE").takeIf { it > 0 } ?: 1,
      replyCount = replyCount,
      maxPage = ((rowCount + POSTS_PER_PAGE - 1) / POSTS_PER_PAGE).coerceAtLeast(1),
      posts = posts,
    )
  }

  private fun buildCommentContentIndex(
    opPost: JSONObject?,
    topic: JSONObject,
    users: JSONObject,
    attachmentBase: String?,
  ): Map<String, NgaThreadEmbeddedReply> {
    if (opPost == null) return emptyMap()
    val comments = opPost.optJSONObject("comment") ?: return emptyMap()
    return comments.keys().asSequence()
      .mapNotNull { key -> comments.optJSONObject(key)?.toEmbeddedReply(topic, users, attachmentBase) }
      .associateBy { it.pid }
  }

  private fun JSONObject.toPost(
    topic: JSONObject,
    users: JSONObject,
    attachmentBase: String?,
    commentContentByPid: Map<String, NgaThreadEmbeddedReply>,
  ): NgaThreadPost {
    val authorId = stringValue("authorid")
    val user = lookupUser(users, authorId)
    val author = resolveAuthorName(user)
    val avatarRaw = user?.nullableStringValue("avatar") ?: nullableStringValue("avatar")
    val memberId = user?.nullableStringValue("memberid", "gid", "groupid")
    val tid = stringValue("tid").ifBlank { topic.stringValue("tid") }
    val pid = stringValue("pid")
    val commentFallback = commentContentByPid[pid]
    val content =
      stringValue("content").ifBlank {
        commentFallback?.content.orEmpty()
      }
    val postDate =
      longValue("postdatetimestamp").takeIf { it > 0 }
        ?: longValue("postdate").takeIf { it > 0 }
        ?: commentFallback?.postDate
        ?: 0L
    val embeddedComments =
      if (intValue("lou") == 0) {
        parseEmbeddedReplies("comment", topic, users, attachmentBase)
      } else {
        emptyList()
      }
    val hotReplies =
      if (intValue("lou") == 0) {
        parseEmbeddedReplies("hotreply", topic, users, attachmentBase)
      } else {
        emptyList()
      }
    return NgaThreadPost(
      pid = pid,
      tid = tid,
      fid = stringValue("fid").ifBlank { topic.stringValue("fid") },
      authorId = authorId,
      author = author,
      authorAvatarUrl = NgaAvatarUrls.resolveUserAvatar(avatarRaw, authorId, memberId),
      authorMemberId = memberId?.toIntOrNull() ?: 0,
      subject = NgaDisplayText.singleLine(stringValue("subject").ifBlank { topic.stringValue("subject") }),
      content = resolveAttachmentContent(content, attachmentBase),
      poll = NgaPollParser.parse(tid, stringValue("vote")),
      score = intValue("score"),
      isOriginalPoster = intValue("lou") == 0 ||
        (authorId.toLongOrNull()?.let { it > 0 } == true && authorId == topic.stringValue("authorid")),
      lou = intValue("lou"),
      postDate = postDate,
      editDate = parseAlterInfo(nullableStringValue("alterinfo")),
      embeddedComments = embeddedComments,
      hotReplies = hotReplies,
      attachments = parseAttachments(attachmentBase),
    )
  }

  private fun JSONObject.parseEmbeddedReplies(
    field: String,
    topic: JSONObject,
    users: JSONObject,
    attachmentBase: String?,
  ): List<NgaThreadEmbeddedReply> {
    val container = optJSONObject(field) ?: return emptyList()
    return container.keys().asSequence()
      .sortedWith(compareBy { it.toIntOrNull() ?: Int.MAX_VALUE })
      .mapNotNull { key -> container.optJSONObject(key)?.toEmbeddedReply(topic, users, attachmentBase) }
      .toList()
  }

  private fun JSONObject.toEmbeddedReply(topic: JSONObject, users: JSONObject, attachmentBase: String?): NgaThreadEmbeddedReply? {
    val content = stringValue("content")
    if (content.isBlank()) return null
    val authorId = stringValue("authorid")
    val user = lookupUser(users, authorId)
    val avatarRaw = user?.nullableStringValue("avatar")
    val memberId = user?.nullableStringValue("memberid", "gid", "groupid")
    return NgaThreadEmbeddedReply(
      pid = stringValue("pid"),
      tid = stringValue("tid").ifBlank { topic.stringValue("tid") },
      authorId = authorId,
      author = resolveAuthorName(user),
      authorAvatarUrl = NgaAvatarUrls.resolveUserAvatar(avatarRaw, authorId, memberId),
      content = resolveAttachmentContent(content, attachmentBase),
      postDate = longValue("postdatetimestamp").takeIf { it > 0 } ?: longValue("postdate"),
      score = intValue("score"),
      lou = intValue("lou"),
    )
  }

  private fun parseAlterInfo(alterinfo: String?): Long? {
    if (alterinfo.isNullOrBlank()) return null
    val match = Regex("""\[E(\d+)""").find(alterinfo) ?: return null
    return match.groupValues[1].toLongOrNull()
  }

  private fun JSONObject.parseAttachments(attachmentBase: String?): List<NgaThreadAttachment> {
    val container = optJSONObject("attachs") ?: optJSONObject("attachments") ?: return emptyList()
    return container.keys().asSequence()
      .sortedWith(compareBy { it.toIntOrNull() ?: Int.MAX_VALUE })
      .mapNotNull { key -> container.opt(key).toAttachment(fallbackName = key, attachmentBase = attachmentBase) }
      .toList()
  }

  private fun Any?.toAttachment(fallbackName: String, attachmentBase: String?): NgaThreadAttachment? =
    when (this) {
      is JSONObject -> {
        val url = nullableStringValue("url", "attachurl", "path", "src", "href")
          ?.let { ImageUrlResolver.resolve(it, attachmentBase) }
          ?.takeIf { it.isNotBlank() }
          ?: return null
        val name = nullableStringValue("name", "filename", "file", "dscp", "desc")
          ?: ImageUrlResolver.fileName(url)
            .takeIf { it.isNotBlank() }
          ?: fallbackName
        NgaThreadAttachment(name = name, url = url)
      }
      is String -> {
        val url = ImageUrlResolver.resolve(this, attachmentBase).takeIf { it.isNotBlank() } ?: return null
        NgaThreadAttachment(name = ImageUrlResolver.fileName(url), url = url)
      }
      else -> null
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

  private const val POSTS_PER_PAGE = 20
}
