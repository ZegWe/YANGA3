package com.yanga.client.data.boards

import com.yanga.client.api.NgaBoardGroup
import com.yanga.client.api.NgaBoardSection
import com.yanga.client.api.NgaBoardSummary
import org.json.JSONArray

/** Parses on-disk cache written from [com.yanga.client.api.NgaBoardCategoryParser] results. */
internal object LocalBoardListParser {
  fun parse(json: String): List<NgaBoardSection> {
    val trimmed = json.trim()
    if (trimmed.isBlank()) return emptyList()
    return parseSections(JSONArray(trimmed))
  }

  private fun parseSections(root: JSONArray): List<NgaBoardSection> =
    buildList {
      for (index in 0 until root.length()) {
        val section = root.optJSONObject(index) ?: continue
        val name = section.optString("name")
        if (name.isBlank()) continue
        val id = section.optString("id").ifBlank { name }
        add(
          NgaBoardSection(
            id = id,
            name = name,
            groups = section.optJSONArray("groups").toBoardGroupList(),
          ),
        )
      }
    }

  private fun JSONArray?.toBoardGroupList(): List<NgaBoardGroup> {
    if (this == null) return emptyList()
    return buildList {
      for (index in 0 until length()) {
        val group = optJSONObject(index) ?: continue
        val name = group.optString("name")
        if (name.isBlank()) continue
        val id = group.optString("id").ifBlank { name }
        add(
          NgaBoardGroup(
            id = id,
            name = name,
            boards = group.optJSONArray("boards").toBoardSummaryList(),
          ),
        )
      }
    }
  }

  private fun JSONArray?.toBoardSummaryList(): List<NgaBoardSummary> {
    if (this == null) return emptyList()
    return buildList {
      for (index in 0 until length()) {
        val board = optJSONObject(index) ?: continue
        val boardId = board.optString("id")
        val name = board.optString("name")
        if (boardId.isBlank() || name.isBlank()) continue
        val iconUrl = board.optString("iconUrl").ifBlank { null }
        add(
          NgaBoardSummary(
            boardId = boardId,
            name = name,
            description = board.optString("description").ifBlank { null },
            todayTopicCount = board.optInt("todayTopicCount").takeIf { board.has("todayTopicCount") },
            unreadCount = board.optInt("unreadCount").takeIf { board.has("unreadCount") },
            isSubscribed = board.optBoolean("isSubscribed", false),
            iconUrl = iconUrl,
          ),
        )
      }
    }
  }
}
