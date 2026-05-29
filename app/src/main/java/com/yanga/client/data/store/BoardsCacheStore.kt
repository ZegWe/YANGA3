package com.yanga.client.data

import android.content.SharedPreferences
import com.yanga.client.api.NgaBoardGroup
import com.yanga.client.api.NgaBoardSection
import com.yanga.client.api.NgaBoardSummary
import org.json.JSONArray
import org.json.JSONObject

interface BoardsCacheStore {
  fun load(cacheKey: String): BoardsReadData?

  fun save(cacheKey: String, data: BoardsReadData)
}

class SharedPreferencesBoardsCacheStore(
  private val preferences: SharedPreferences,
  private val nowProvider: () -> Long = { System.currentTimeMillis() },
  private val ttlMs: Long = DEFAULT_TTL_MS,
) : BoardsCacheStore {
  override fun load(cacheKey: String): BoardsReadData? {
    val root = readRoot(cacheKey) ?: return null
    val timestamp = root.optLong(KEY_TIMESTAMP, 0L)
    if (timestamp <= 0L || nowProvider() - timestamp > ttlMs) return null
    val data = parseBoards(root)
    return data.takeIf { it.remoteSections.isNotEmpty() }
  }

  override fun save(cacheKey: String, data: BoardsReadData) {
    val payload = JSONObject()
      .put(KEY_TIMESTAMP, nowProvider())
      .put(KEY_SUBSCRIBED_BOARDS, data.subscribedBoards.toBoardSummaryJsonArray())
      .put(KEY_SECTIONS, data.remoteSections.toBoardSectionJsonArray())
    preferences.edit().putString(prefKey(cacheKey), payload.toString()).apply()
  }

  private fun readRoot(cacheKey: String): JSONObject? {
    val raw = preferences.getString(prefKey(cacheKey), null).orEmpty()
    if (raw.isBlank()) return null
    return runCatching { JSONObject(raw) }.getOrNull()
  }

  private fun parseBoards(root: JSONObject): BoardsReadData {
    val subscribedBoards = root.optJSONArray(KEY_SUBSCRIBED_BOARDS).toBoardSummaryList()
    val sections = root.optJSONArray(KEY_SECTIONS).toBoardSectionList()
    return BoardsReadData(
      subscribedBoards = subscribedBoards,
      remoteSections = sections,
    )
  }

  private fun prefKey(cacheKey: String): String = "$KEY_BOARDS_CACHE_PREFIX$cacheKey"

  private companion object {
    const val KEY_BOARDS_CACHE_PREFIX = "boards_cache_"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_SUBSCRIBED_BOARDS = "subscribedBoards"
    const val KEY_SECTIONS = "sections"
    const val KEY_GROUPS = "groups"
    const val KEY_BOARDS = "boards"
    const val KEY_ID = "id"
    const val KEY_NAME = "name"
    const val KEY_DESCRIPTION = "description"
    const val KEY_TODAY_TOPIC_COUNT = "todayTopicCount"
    const val KEY_UNREAD_COUNT = "unreadCount"
    const val KEY_IS_SUBSCRIBED = "isSubscribed"
    const val KEY_ICON_URL = "iconUrl"
    const val DEFAULT_TTL_MS = 30 * 60 * 1000L
  }

  private fun JSONArray?.toBoardSummaryList(): List<NgaBoardSummary> {
    if (this == null) return emptyList()
    return buildList {
      for (index in 0 until length()) {
        val item = optJSONObject(index) ?: continue
        val boardId = item.optString(KEY_ID)
        val name = item.optString(KEY_NAME)
        if (boardId.isBlank() || name.isBlank()) continue
        add(
          NgaBoardSummary(
            boardId = boardId,
            name = name,
            description = item.optString(KEY_DESCRIPTION).ifBlank { null },
            todayTopicCount = item.optInt(KEY_TODAY_TOPIC_COUNT).takeIf { item.has(KEY_TODAY_TOPIC_COUNT) },
            unreadCount = item.optInt(KEY_UNREAD_COUNT).takeIf { item.has(KEY_UNREAD_COUNT) },
            isSubscribed = item.optBoolean(KEY_IS_SUBSCRIBED, false),
            iconUrl = item.optString(KEY_ICON_URL).ifBlank { null },
          ),
        )
      }
    }
  }

  private fun JSONArray?.toBoardSectionList(): List<NgaBoardSection> {
    if (this == null) return emptyList()
    return buildList {
      for (index in 0 until length()) {
        val item = optJSONObject(index) ?: continue
        val id = item.optString(KEY_ID)
        val name = item.optString(KEY_NAME)
        if (id.isBlank() || name.isBlank()) continue
        add(
          NgaBoardSection(
            id = id,
            name = name,
            groups = item.optJSONArray(KEY_GROUPS).toBoardGroupList(),
          ),
        )
      }
    }
  }

  private fun JSONArray?.toBoardGroupList(): List<NgaBoardGroup> {
    if (this == null) return emptyList()
    return buildList {
      for (index in 0 until length()) {
        val item = optJSONObject(index) ?: continue
        val id = item.optString(KEY_ID)
        val name = item.optString(KEY_NAME)
        if (id.isBlank() || name.isBlank()) continue
        add(
          NgaBoardGroup(
            id = id,
            name = name,
            boards = item.optJSONArray(KEY_BOARDS).toBoardSummaryList(),
          ),
        )
      }
    }
  }

  private fun List<NgaBoardSummary>.toBoardSummaryJsonArray(): JSONArray {
    val array = JSONArray()
    forEach { board ->
      array.put(
        JSONObject()
          .put(KEY_ID, board.boardId)
          .put(KEY_NAME, board.name)
          .put(KEY_DESCRIPTION, board.description.orEmpty())
          .apply { board.todayTopicCount?.let { put(KEY_TODAY_TOPIC_COUNT, it) } }
          .apply { board.unreadCount?.let { put(KEY_UNREAD_COUNT, it) } }
          .put(KEY_IS_SUBSCRIBED, board.isSubscribed)
          .put(KEY_ICON_URL, board.iconUrl.orEmpty()),
      )
    }
    return array
  }

  private fun List<NgaBoardSection>.toBoardSectionJsonArray(): JSONArray {
    val array = JSONArray()
    forEach { section ->
      array.put(
        JSONObject()
          .put(KEY_ID, section.id)
          .put(KEY_NAME, section.name)
          .put(KEY_GROUPS, section.groups.toBoardGroupJsonArray()),
      )
    }
    return array
  }

  private fun List<NgaBoardGroup>.toBoardGroupJsonArray(): JSONArray {
    val array = JSONArray()
    forEach { group ->
      array.put(
        JSONObject()
          .put(KEY_ID, group.id)
          .put(KEY_NAME, group.name)
          .put(KEY_BOARDS, group.boards.toBoardSummaryJsonArray()),
      )
    }
    return array
  }
}
