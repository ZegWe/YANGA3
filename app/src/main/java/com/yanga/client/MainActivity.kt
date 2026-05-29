package com.yanga.client

import android.content.Context
import android.os.Bundle
import android.webkit.CookieManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yanga.client.theme.YangaTheme
import com.yanga.client.ui.LoginSessionUiState
import com.yanga.client.ui.MainScreen
import com.yanga.client.data.SharedPreferencesBoardsCacheStore
import com.yanga.client.data.DefaultNgaReadOnlyRepository
import com.yanga.client.data.SharedPreferencesFavoriteBoardsStore

class MainActivity : ComponentActivity() {
  private var loginSession by mutableStateOf<LoginSessionUiState?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    loginSession = loadLoginSession()

    enableEdgeToEdge()
    setContent {
      val repository = remember {
        val preferences = getPreferences(Context.MODE_PRIVATE)
        DefaultNgaReadOnlyRepository(
          favoriteBoardsStore = SharedPreferencesFavoriteBoardsStore(preferences),
          boardsCacheStore = SharedPreferencesBoardsCacheStore(preferences),
        )
      }
      YangaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
          MainScreen(
            loginSession = loginSession,
            repository = repository,
            onLoginComplete = { session ->
              loginSession = session
              saveLoginSession(session)
            },
            onLogout = {
              CookieManager.getInstance().removeAllCookies(null)
              CookieManager.getInstance().flush()
              loginSession = null
              clearLoginSession()
            },
          )
        }
      }
    }
  }

  private fun saveLoginSession(session: LoginSessionUiState) {
    getPreferences(Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_USERNAME, session.username)
      .putString(KEY_UID, session.uid)
      .putString(KEY_COOKIE, session.cookie)
      .apply()
  }

  private fun loadLoginSession(): LoginSessionUiState? {
    val preferences = getPreferences(Context.MODE_PRIVATE)
    val username = preferences.getString(KEY_USERNAME, null).orEmpty()
    val uid = preferences.getString(KEY_UID, null).orEmpty()
    val cookie = preferences.getString(KEY_COOKIE, null).orEmpty()
    return if (username.isBlank() || uid.isBlank() || cookie.isBlank()) {
      null
    } else {
      LoginSessionUiState(username = username, uid = uid, cookie = cookie)
    }
  }

  private fun clearLoginSession() {
    getPreferences(Context.MODE_PRIVATE).edit().clear().apply()
  }

  private companion object {
    const val KEY_USERNAME = "username"
    const val KEY_UID = "uid"
    const val KEY_COOKIE = "cookie"
  }
}

