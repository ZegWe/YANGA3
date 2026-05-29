package com.yanga.client.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class SharedPreferencesFavoriteBoardsStore(
  private val preferences: SharedPreferences,
) : FavoriteBoardsStore {
  override fun list(): List<LocalFavoriteBoard> {
    val raw = preferences.getString(KEY_LOCAL_FAVORITES, null).orEmpty()
    if (raw.isBlank()) return emptyList()
    return runCatching {
      val array = JSONArray(raw)
      buildList {
        for (index in 0 until array.length()) {
          val item = array.optJSONObject(index) ?: continue
          val boardId = item.optString(KEY_BOARD_ID)
          val name = item.optString(KEY_NAME)
          if (boardId.isBlank() || name.isBlank()) continue
          add(
            LocalFavoriteBoard(
              boardId = boardId,
              name = name,
              iconUrl = item.optString(KEY_ICON_URL).ifBlank { null },
              category = item.optString(KEY_CATEGORY),
            ),
          )
        }
      }
    }.getOrElse { emptyList() }
  }

  override fun upsert(board: LocalFavoriteBoard) {
    val current = list().associateBy { it.boardId }.toMutableMap()
    current[board.boardId] = board
    persist(current.values.toList())
  }

  override fun remove(boardId: String) {
    val current = list().associateBy { it.boardId }.toMutableMap()
    current.remove(boardId)
    persist(current.values.toList())
  }

  private fun persist(boards: List<LocalFavoriteBoard>) {
    val array = JSONArray()
    boards.forEach { board ->
      array.put(
        JSONObject()
          .put(KEY_BOARD_ID, board.boardId)
          .put(KEY_NAME, board.name)
          .put(KEY_ICON_URL, board.iconUrl ?: "")
          .put(KEY_CATEGORY, board.category),
      )
    }
    preferences.edit().putString(KEY_LOCAL_FAVORITES, array.toString()).apply()
  }

  private companion object {
    const val KEY_LOCAL_FAVORITES = "local_favorite_boards"
    const val KEY_BOARD_ID = "boardId"
    const val KEY_NAME = "name"
    const val KEY_ICON_URL = "icon"
    const val KEY_CATEGORY = "category"
  }
}
