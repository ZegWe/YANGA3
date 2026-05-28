package com.yanga.client.ui.main

import com.yanga.client.data.LoginSessionData

enum class MainTab(val label: String) {
  Home("Home"),
  Boards("Boards"),
  Messages("Messages"),
  Profile("Profile"),
}

data class BoardPreview(
  val name: String,
  val metadata: String,
  val marker: String,
  val badge: String? = null,
)

data class TopicPreview(
  val title: String,
  val board: String,
  val replies: String,
  val lastActive: String,
  val authorInitial: String,
)

data class MessagePreview(
  val contact: String,
  val preview: String,
  val time: String,
  val badge: String? = null,
)

data class SettingsPreview(
  val icon: String,
  val title: String,
  val subtitle: String,
  val badge: String? = null,
)

sealed interface LoadableUiState<out T> {
  object Loading : LoadableUiState<Nothing>

  data class Error(
    val message: String,
    val cause: Throwable? = null,
  ) : LoadableUiState<Nothing>

  data class Empty(
    val message: String,
  ) : LoadableUiState<Nothing>

  object LoginRequired : LoadableUiState<Nothing>

  data class Content<T>(
    val value: T,
  ) : LoadableUiState<T>
}

data class HomeUiState(
  val boards: LoadableUiState<List<BoardPreview>> = LoadableUiState.Loading,
  val activeTopics: LoadableUiState<List<TopicPreview>> = LoadableUiState.Loading,
)

data class BoardsUiState(
  val subscribedBoards: LoadableUiState<List<BoardPreview>> = LoadableUiState.Loading,
  val categories: LoadableUiState<List<BoardPreview>> = LoadableUiState.Loading,
)

data class MessagesUiState(
  val messages: LoadableUiState<List<MessagePreview>> = LoadableUiState.LoginRequired,
)

data class ProfileUiState(
  val session: LoadableUiState<LoginSessionData> = LoadableUiState.LoginRequired,
  val counters: LoadableUiState<List<SettingsPreview>> = LoadableUiState.LoginRequired,
  val notifications: LoadableUiState<List<SettingsPreview>> = LoadableUiState.LoginRequired,
  val settingsRows: List<SettingsPreview> = defaultSettingsRows,
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

internal val favoriteBoards: List<BoardPreview> = emptyList()

internal val activeTopics: List<TopicPreview> = emptyList()

internal val subscribedBoards: List<BoardPreview> = emptyList()

internal val forumCategories: List<BoardPreview> = emptyList()

internal val privateMessages: List<MessagePreview> = emptyList()

internal val settingsRows: List<SettingsPreview> = ProfileUiState.defaultSettingsRows

data class MainContentUiState(
  val home: HomeUiState = HomeUiState(),
  val boards: BoardsUiState = BoardsUiState(),
  val messages: MessagesUiState = MessagesUiState(),
  val profile: ProfileUiState = ProfileUiState(),
) {
  val isLoggedIn: Boolean
    get() = profile.session is LoadableUiState.Content

  companion object {
    fun initialLoggedOut(): MainContentUiState =
      MainContentUiState(
        home = HomeUiState(),
        boards = BoardsUiState(),
        messages =
          MessagesUiState(
            messages = LoadableUiState.LoginRequired,
          ),
        profile =
          ProfileUiState(
            session = LoadableUiState.LoginRequired,
            counters = LoadableUiState.LoginRequired,
            notifications = LoadableUiState.LoginRequired,
          ),
      )

    fun initialLoggedIn(session: LoginSessionData): MainContentUiState =
      MainContentUiState(
        home = HomeUiState(),
        boards = BoardsUiState(),
        messages =
          MessagesUiState(
            messages = LoadableUiState.Loading,
          ),
        profile =
          ProfileUiState(
            session = LoadableUiState.Content(session),
            counters = LoadableUiState.Loading,
            notifications = LoadableUiState.Loading,
          ),
      )
  }
}
