package com.yanga.client

import androidx.compose.runtime.Composable
import com.yanga.client.ui.MainScreen

class MainActivity : YangaComposeActivity() {
  @Composable
  override fun Content() {
    MainScreen(
      loginSession = loginSession,
      repository = app.repository,
      boardsCatalog = app.boardsCatalog,
      app = app,
      onLogout = ::onLogout,
    )
  }
}
