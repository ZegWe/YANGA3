package com.yanga.client.data

import android.content.SharedPreferences

interface SubBoardFilterStore {
  fun load(boardFid: String): Set<String>

  fun save(boardFid: String, selectedIds: Set<String>)
}

class SharedPreferencesSubBoardFilterStore(
  private val preferences: SharedPreferences,
) : SubBoardFilterStore {
  override fun load(boardFid: String): Set<String> {
    val raw = preferences.getString(prefKey(boardFid), null).orEmpty()
    if (raw.isBlank()) return emptySet()
    return raw.split(',').filter { it.isNotBlank() }.toSet()
  }

  override fun save(boardFid: String, selectedIds: Set<String>) {
    preferences.edit().apply {
      if (selectedIds.isEmpty()) {
        remove(prefKey(boardFid))
      } else {
        putString(prefKey(boardFid), selectedIds.joinToString(","))
      }
    }.apply()
  }

  private fun prefKey(boardFid: String): String = "$KEY_PREFIX$boardFid"

  private companion object {
    const val KEY_PREFIX = "sub_board_filter_"
  }
}
