package com.yanga.client

import android.os.Bundle
import android.webkit.CookieManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yanga.client.theme.ThemePreferences
import com.yanga.client.theme.ThemePreferencesStore
import com.yanga.client.theme.YangaTheme
import com.yanga.client.ui.LoginSessionUiState

abstract class YangaComposeActivity : ComponentActivity() {
  protected var loginSession by mutableStateOf<LoginSessionUiState?>(null)
    private set
  protected var themePreferences by mutableStateOf(ThemePreferences())
    private set

  protected val app: YangaApplication
    get() = application as YangaApplication

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    loginSession = LoginSessionStore.load(this)
    themePreferences = ThemePreferencesStore.load(this)
    enableEdgeToEdge()
    setContent {
      YangaTheme(themePreferences = themePreferences) {
        Surface(color = MaterialTheme.colorScheme.background) {
          Content()
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    val latest = LoginSessionStore.load(this)
    if (latest != loginSession) {
      loginSession = latest
    }
  }

  @Composable
  protected abstract fun Content()

  protected fun onLoginComplete(session: LoginSessionUiState) {
    loginSession = session
    LoginSessionStore.save(this, session)
  }

  protected fun onLogout() {
    CookieManager.getInstance().removeAllCookies(null)
    CookieManager.getInstance().flush()
    loginSession = null
    LoginSessionStore.clear(this)
  }

  protected fun updateThemePreferences(preferences: ThemePreferences) {
    themePreferences = preferences
    ThemePreferencesStore.save(this, preferences)
  }
}
