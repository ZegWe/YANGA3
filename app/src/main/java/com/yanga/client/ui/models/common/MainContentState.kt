package com.yanga.client.ui

import com.yanga.client.data.LoginSessionData

data class MainContentUiState(
  val home: HomeUiState = HomeUiState(),
  val boards: BoardsUiState = BoardsUiState(),
  val messages: MessagesUiState = MessagesUiState(),
  val profile: ProfileUiState = ProfileUiState(),
  val activeBoard: BoardTopicListUiState? = null,
  val activeThread: ThreadUiState? = null,
) {
  val isLoggedIn: Boolean
    get() = profile.session is LoadableUiState.Content

  companion object {
    fun initialLoggedOut(): MainContentUiState =
      MainContentUiState(
        home = HomeUiState(),
        boards = BoardsUiState(),
        messages = MessagesUiState(messages = LoadableUiState.LoginRequired),
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
        messages = MessagesUiState(messages = LoadableUiState.Loading),
        profile =
          ProfileUiState(
            session = LoadableUiState.Content(session),
            counters = LoadableUiState.Loading,
            notifications = LoadableUiState.Loading,
          ),
      )
  }
}

sealed interface MainContentNavigationEvent {
  data class OpenBoard(val destination: BoardDestination) : MainContentNavigationEvent
  data class OpenThread(val destination: ThreadDestination) : MainContentNavigationEvent
  data object BackFromThread : MainContentNavigationEvent
  data object BackFromBoard : MainContentNavigationEvent
}
