package com.yanga.client.api

import org.json.JSONObject

object NgaBoardCategoryParser {
  fun parse(raw: String): List<NgaBoardCategory> {
    val root = ngaJsonRoot(raw)
    val data = root.objectValue("data") ?: root
    return data.objectListValue("categories", "category", "list").mapNotNull { category ->
      category.toBoardCategory()
    }
  }

  private fun JSONObject.toBoardCategory(): NgaBoardCategory? {
    val id = stringValue("id", "cid", "category_id")
    val name = stringValue("name", "title")
    val boards = objectListValue("boards", "forums", "children", "list").mapNotNull { board ->
      board.toBoardSummary()
    }
    if (id.isBlank() && name.isBlank() && boards.isEmpty()) return null

    return NgaBoardCategory(
      id = id,
      name = name,
      boards = boards,
    )
  }

  private fun JSONObject.toBoardSummary(): NgaBoardSummary? {
    val boardId = stringValue("fid", "id", "board_id", "boardId")
    val name = stringValue("name", "title")
    if (boardId.isBlank() && name.isBlank()) return null

    return NgaBoardSummary(
      boardId = boardId,
      name = name,
      description = nullableStringValue("description", "desc"),
      todayTopicCount = nullableIntValue("todayposts", "today_topic_count", "todayTopicCount"),
      unreadCount = nullableIntValue("unread", "unread_count", "unreadCount"),
      isSubscribed = booleanValue("subscribed", "is_subscribed", "isSubscribed"),
    )
  }
}
