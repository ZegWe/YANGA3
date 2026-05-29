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
        SettingsPreview("阅", "Reading and appearance", "字体、主题、图片加载"),
        SettingsPreview("缓", "Cache and history", "最近阅读、离线缓存"),
        SettingsPreview("屏", "Block words", "过滤内容和用户"),
      )
  }
}

internal val settingsRows: List<SettingsPreview> = ProfileUiState.defaultSettingsRows
