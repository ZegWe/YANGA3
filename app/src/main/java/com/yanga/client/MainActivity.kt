package com.yanga.client

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yanga.client.ui.MainDestinationKey
import com.yanga.client.ui.navigation.threadDeepLink
import androidx.compose.runtime.Composable
import com.yanga.client.ui.MainScreen

class MainActivity : YangaComposeActivity() {
  private var pendingDestination by mutableStateOf<MainDestinationKey?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    if (savedInstanceState == null) pendingDestination = threadDeepLink(intent.dataString)
    super.onCreate(savedInstanceState)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    pendingDestination = threadDeepLink(intent.dataString)
  }

  @Composable
  override fun Content() {
    MainScreen(
      loginSession = loginSession,
      repository = app.repository,
      boardsCatalog = app.boardsCatalog,
      app = app,
      onLogout = ::onLogout,
      onLoginComplete = ::onLoginComplete,
      pendingDestination = pendingDestination,
      onDestinationConsumed = { pendingDestination = null },
      themePreferences = themePreferences,
      onThemePreferencesChange = ::updateThemePreferences,
    )
  }
}
