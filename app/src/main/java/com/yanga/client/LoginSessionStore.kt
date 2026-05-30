package com.yanga.client

import android.content.Context
import com.yanga.client.ui.LoginSessionUiState

object LoginSessionStore {
  private const val PREFS_NAME = "yanga_prefs"
  private const val KEY_USERNAME = "username"
  private const val KEY_UID = "uid"
  private const val KEY_COOKIE = "cookie"

  fun load(context: Context): LoginSessionUiState? {
    val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val username = preferences.getString(KEY_USERNAME, null).orEmpty()
    val uid = preferences.getString(KEY_UID, null).orEmpty()
    val cookie = preferences.getString(KEY_COOKIE, null).orEmpty()
    return if (username.isBlank() || uid.isBlank() || cookie.isBlank()) {
      null
    } else {
      LoginSessionUiState(username = username, uid = uid, cookie = cookie)
    }
  }

  fun save(context: Context, session: LoginSessionUiState) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_USERNAME, session.username)
      .putString(KEY_UID, session.uid)
      .putString(KEY_COOKIE, session.cookie)
      .apply()
  }

  fun clear(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
  }
}
