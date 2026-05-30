package com.yanga.client.api

import com.yanga.client.data.image.ImageUrlResolver

object NgaAvatarUrls {
  private const val AVATAR_BASE = "https://img4.nga.178.com/avatars/2002"
  private val RELATIVE_AVATAR =
    Regex("""^\.a/(\d+)_(\d+)\.(jpg|png|gif)\?(\d+)""", RegexOption.IGNORE_CASE)

  fun resolveUserAvatar(
    raw: String?,
    userId: String = "",
    memberId: String? = null,
  ): String? =
    resolve(raw, userId) ?: defaultMemberAvatar(memberId)

  fun defaultMemberAvatar(memberId: String?): String? {
    val id = memberId?.trim()?.toIntOrNull() ?: return null
    if (id <= 0) return null
    return "${NgaStaticUrls.emoticonBaseUrl}ac$id.png"
  }

  fun resolve(raw: String?, authorId: String = ""): String? {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return null

    RELATIVE_AVATAR.matchEntire(value)?.let { match ->
      val attachmentId = match.groupValues[1]
      val version = match.groupValues[2]
      val extension = match.groupValues[3]
      val cacheKey = match.groupValues[4]
      val shard = attachmentId.toLongOrNull()?.let(::shardPath) ?: return null
      return "$AVATAR_BASE/$shard/${attachmentId}_${version}.$extension?$cacheKey"
    }

    extractHttpUrl(value)?.let { url ->
      return ImageUrlResolver.resolve(url).takeIf { it.isNotBlank() }
    }

    return null
  }

  internal fun extractHttpUrl(value: String): String? {
    val start = value.indexOf("http")
    if (start < 0) return null
    val slice = value.substring(start)
    val endCandidates = listOf(slice.indexOf('"'), slice.indexOf('|')).filter { it >= 0 }
    val end = endCandidates.minOrNull() ?: slice.length
    return slice.substring(0, end).trim().takeIf { it.isNotBlank() }
  }

  private fun shardPath(attachmentId: Long): String {
    val padded = "000000000${attachmentId.toString(16)}"
    val match =
      Regex("""([0-9a-z]{3})([0-9a-z]{3})([0-9a-z]{3})$""").find(padded)
        ?: return "000/000/000"
    return "${match.groupValues[3]}/${match.groupValues[2]}/${match.groupValues[1]}"
  }
}
