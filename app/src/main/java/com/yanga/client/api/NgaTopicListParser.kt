package com.yanga.client.api

import org.json.JSONObject

object NgaTopicListParser {
  fun parse(raw: String): NgaTopicList {
    val root = ngaJsonRoot(raw)
    val data = root.objectValue("data") ?: root
    val boardMeta = data.objectValue("__F")
    val boardFid = boardMeta?.stringValue("fid", "id").orEmpty()
    val users = data.objectValue("__U")
    val topics =
      data.objectListValue("__T", "topics", "list").mapNotNull { topic ->
        topic.toTopicSummary(users = users, mainBoardFid = boardFid)
      }
    val page = data.intValue("__PAGE", "page").takeIf { it > 0 } ?: 1

    return NgaTopicList(
      topics = topics,
      page = page,
      hasNextPage = data.booleanValue("has_next_page", "hasNextPage", "next")
        || data.intValue("__ROWS", "rows", "total") > topics.size,
      subBoards = parseSubBoards(data),
      boardFid = boardFid,
    )
  }

  private fun parseSubBoards(data: JSONObject): List<NgaSubBoard> {
    val subForums = data.objectValue("__F")?.objectValue("sub_forums") ?: return emptyList()
    return subForums.keys().asSequence().mapNotNull { key ->
      val entry = subForums.optJSONObject(key) ?: return@mapNotNull null
      val name =
        entry.optString("1").ifBlank {
          entry.stringValue("name", "title")
        }
      if (name.isBlank()) return@mapNotNull null
      NgaSubBoard(
        id = key,
        name = NgaDisplayText.singleLine(name),
        valueId = entry.opt("0")?.toString()?.takeIf { it.isNotBlank() } ?: key.removePrefix("t"),
        // user_option add_to_block_tids uses thread/board ids aligned with sub_forums[3]/[0],
        // not the auxiliary marker in sub_forums[4].
        subscribeId =
          entry.opt("3")?.toString()?.takeIf { it.isNotBlank() }
            ?: entry.opt("0")?.toString()?.takeIf { it.isNotBlank() },
      )
    }.sortedBy { it.name }.toList()
  }

  private fun JSONObject.toTopicSummary(users: JSONObject?, mainBoardFid: String): NgaTopicSummary? {
    val topicId = stringValue("tid", "topic_id", "topicId", "id")
    if (topicId.isBlank()) return null

    val authorId = nullableStringValue("authorid", "author_id", "authorId", "uid")
    val user = lookupUser(users, authorId)
    val authorName =
      nullableStringValue("author", "username", "author_name", "authorName")
        ?: user?.nullableStringValue("username", "nickname")
    val avatarRaw = user?.nullableStringValue("avatar") ?: nullableStringValue("avatar")
    val memberId = user?.nullableStringValue("memberid", "gid", "groupid")
    val topicFid = stringValue("fid", "board_id", "boardId")
    val parent = optJSONObject("parent")
    val parentFid = parent?.opt("0")?.toString()?.takeIf { it.isNotBlank() }
    val parentCategoryTopicId = parent?.opt("1")?.toString()?.takeIf { it.isNotBlank() }
    val miscCategoryTopicId =
      optJSONObject("topic_misc_var")
        ?.opt("2")
        ?.toString()
        ?.takeIf { it.isNotBlank() }
    val entryTarget = topicEntryTarget(topicId)
    val categoryTopicId = parentCategoryTopicId ?: miscCategoryTopicId
    val subForumFid =
      when {
        parentCategoryTopicId != null -> null
        mainBoardFid.isBlank() -> null
        parentFid != null && parentFid != mainBoardFid -> parentFid
        topicFid.isNotBlank() && topicFid != mainBoardFid -> topicFid
        else -> null
      }

    return NgaTopicSummary(
      topicId = topicId,
      boardId = topicFid,
      boardName = NgaDisplayText.singleLine(
        stringValue("fname", "forumname", "forum_name", "board_name", "boardName")
          .ifBlank { parent?.stringValue("2").orEmpty() },
      ),
      title = NgaDisplayText.singleLine(stringValue("subject", "title")),
      authorId = authorId,
      authorName = authorName?.let(NgaDisplayText::singleLine),
      authorAvatarUrl = NgaAvatarUrls.resolveUserAvatar(avatarRaw, authorId.orEmpty(), memberId),
      replyCount = intValue("replies", "reply_count", "replyCount"),
      lastPostAt = nullableLongValue("lastpost", "last_post_at", "lastPostAt", "postdatetimestamp", "postdate"),
      isFavorited = booleanValue("favor", "is_favorited", "isFavorited", "favorited"),
      subForumFid = subForumFid,
      categoryTopicId = categoryTopicId,
      entryTarget = entryTarget,
      titleStyle = NgaTitleStyleParser.parse(
        misc = stringValue("topic_misc"),
        fontBits = optJSONObject("topic_misc_var")?.opt("1")?.toString()?.toIntOrNull(),
        titleFont = stringValue("titlefont"),
      ),
      isLocked = intValue("type") and 1024 != 0,
      hasAttachments = intValue("type") and 8192 != 0,
    )
  }

  private fun JSONObject.topicEntryTarget(topicId: String): NgaTopicEntryTarget? {
    val misc = optJSONObject("topic_misc_var")
    when (misc?.intValue("1")) {
      32 -> {
        val boardId = misc.opt("3")?.toString()?.takeIf { it.isNotBlank() } ?: return null
        return NgaTopicEntryTarget(id = boardId, type = NgaTopicEntryType.Board)
      }
      33 -> return NgaTopicEntryTarget(id = "t$topicId", type = NgaTopicEntryType.Collection)
    }

    return if ((intValue("type") and COLLECTION_TOPIC_TYPE_MASK) == COLLECTION_TOPIC_TYPE_MASK) {
      NgaTopicEntryTarget(id = "t$topicId", type = NgaTopicEntryType.Collection)
    } else {
      null
    }
  }

  private fun JSONObject.nullableStringValue(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key ->
      opt(key)?.takeUnless { it == JSONObject.NULL }?.toString()?.takeIf { it.isNotBlank() }
    }

  private const val COLLECTION_TOPIC_TYPE_MASK = 32768
}
