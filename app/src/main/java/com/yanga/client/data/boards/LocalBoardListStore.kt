package com.yanga.client.data.boards

import android.content.Context
import android.content.SharedPreferences
import com.yanga.client.api.NgaBoardSection
import java.io.File

/** Runtime disk cache for the board directory (written only after a successful API fetch). */
class LocalBoardListStore(
  private val context: Context,
  private val preferences: SharedPreferences,
) : BoardSectionDirectory {
  override fun loadSections(): List<NgaBoardSection> {
    val dataFile = dataFile()
    checkLocalDataVersion(dataFile)
    if (!dataFile.exists()) return emptyList()
    return BoardListJsonCodec.decode(dataFile.readText())
  }

  override fun saveSections(sections: List<NgaBoardSection>) {
    if (sections.isEmpty()) return
    writeDataFile(BoardListJsonCodec.encode(sections))
    preferences.edit().putInt(KEY_LOCAL_VERSION, CURRENT_LOCAL_VERSION).apply()
  }

  override fun clear() {
    dataFile().delete()
    preferences.edit()
      .putLong(KEY_INCREMENTAL_REQUEST_AT, 0L)
      .apply()
  }

  override fun lastIncrementalRequestAt(): Long =
    preferences.getLong(KEY_INCREMENTAL_REQUEST_AT, 0L)

  override fun markIncrementalRequested(at: Long) {
    preferences.edit().putLong(KEY_INCREMENTAL_REQUEST_AT, at).apply()
  }

  private fun dataFile(): File = File(context.filesDir, BOARD_FILE_NAME)

  private fun writeDataFile(payload: String) {
    dataFile().writeText(payload)
  }

  private fun checkLocalDataVersion(file: File) {
    val currentVersion = preferences.getInt(KEY_LOCAL_VERSION, 0)
    if (currentVersion != CURRENT_LOCAL_VERSION) {
      preferences.edit()
        .putInt(KEY_LOCAL_VERSION, CURRENT_LOCAL_VERSION)
        .putLong(KEY_INCREMENTAL_REQUEST_AT, 0L)
        .apply()
      if (file.exists()) {
        file.delete()
      }
    }
  }

  private companion object {
    const val BOARD_FILE_NAME = "board_list.json"
    const val KEY_LOCAL_VERSION = "board_local_version"
    const val KEY_INCREMENTAL_REQUEST_AT = "board_remote_request_time"
    const val CURRENT_LOCAL_VERSION = 3
  }
}
