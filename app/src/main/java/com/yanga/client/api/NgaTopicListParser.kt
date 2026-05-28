package com.yanga.client.api

import org.json.JSONObject

object NgaTopicListParser {
  fun parse(raw: String): NgaTopicList {
    val root = ngaJsonRoot(raw)
    val data = root.objectValue("data") ?: root
    val topics = data.objectListValue("__T", "topics", "list").mapNotNull { topic ->
      topic.toTopicSummary()
    }
    val page = data.intValue("__PAGE", "page").takeIf { it > 0 } ?: 1

    return NgaTopicList(
      topics = topics,
      page = page,
      hasNextPage = data.booleanValue("has_next_page", "hasNextPage", "next")
        || data.intValue("__ROWS", "rows", "total") > topics.size,
    )
  }

  private fun JSONObject.toTopicSummary(): NgaTopicSummary? {
    val topicId = stringValue("tid", "topic_id", "topicId", "id")
    if (topicId.isBlank()) return null

    return NgaTopicSummary(
      topicId = topicId,
      boardId = stringValue("fid", "board_id", "boardId"),
      boardName = stringValue("fname", "forumname", "forum_name", "board_name", "boardName"),
      title = stringValue("subject", "title"),
      authorId = nullableStringValue("authorid", "author_id", "authorId", "uid"),
      authorName = nullableStringValue("author", "username", "author_name", "authorName"),
      replyCount = intValue("replies", "reply_count", "replyCount"),
      lastPostAt = nullableLongValue("lastpost", "last_post_at", "lastPostAt", "postdatetimestamp", "postdate"),
      isFavorited = booleanValue("favor", "is_favorited", "isFavorited", "favorited"),
    )
  }
}
