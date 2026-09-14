package com.yanga.client.ui

import com.yanga.client.api.NgaDomains
import com.yanga.client.data.LoginSessionData

data class SettingsPreview(
  val icon: String,
  val title: String,
  val subtitle: String,
  val badge: String? = null,
)

data class ProfileUiState(
  val session: LoadableUiState<LoginSessionData> = LoadableUiState.LoginRequired,
  val counters: LoadableUiState<List<SettingsPreview>> = LoadableUiState.LoginRequired,
  val notifications: LoadableUiState<List<SettingsPreview>> = LoadableUiState.LoginRequired,
  val settingsRows: List<SettingsPreview> = defaultSettingsRows,
  val forumEndpoint: String = NgaDomains.BBS_NGA_CN,
) {
  companion object {
    val defaultSettingsRows =
      listOf(
        SettingsPreview("about", "关于", "应用版本与更新"),
        SettingsPreview("theme", "主题", "跟随系统、浅色、深色"),
        SettingsPreview("endpoint", "设置端点", NgaDomains.BBS_NGA_CN),
      )
  }
}

internal val settingsRows: List<SettingsPreview> = ProfileUiState.defaultSettingsRows
