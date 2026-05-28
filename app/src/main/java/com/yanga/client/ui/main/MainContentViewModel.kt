package com.yanga.client.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yanga.client.data.BoardsReadData
import com.yanga.client.data.HomeReadData
import com.yanga.client.data.LoginRequiredException
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.MessagesReadData
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.data.ProfileReadData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainContentViewModel(
  private val repository: NgaReadOnlyRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(MainContentUiState.initialLoggedOut())
  val uiState: StateFlow<MainContentUiState> = _uiState.asStateFlow()

  fun refresh(loginSession: LoginSessionUiState?) {
    val session = loginSession?.toData()
    _uiState.value =
      if (session == null) {
        MainContentUiState.initialLoggedOut()
      } else {
        MainContentUiState.initialLoggedIn(session)
      }

    viewModelScope.launch {
      val homeResult = repository.loadHome()
      _uiState.update { state -> state.withHomeResult(homeResult) }

      val boardsResult = repository.loadBoards(session)
      _uiState.update { state -> state.withBoardsResult(boardsResult) }

      if (session != null) {
        val messagesResult = repository.loadMessages(session)
        _uiState.update { state -> state.withMessagesResult(messagesResult) }

        val profileResult = repository.loadProfile(session)
        _uiState.update { state -> state.withProfileResult(session, profileResult) }
      }
    }
  }
}

internal fun LoginSessionUiState.toData(): LoginSessionData =
  LoginSessionData(
    username = username,
    uid = uid,
    cookie = cookie,
  )

private fun MainContentUiState.withHomeResult(result: Result<HomeReadData>): MainContentUiState =
  result.fold(
    onSuccess = { data ->
      copy(
        home =
          home.copy(
            boards = LoadableUiState.Content(data.boards.map { it.toPreview() }),
            activeTopics = LoadableUiState.Content(data.activeTopics.map { it.toPreview() }),
          ),
      )
    },
    onFailure = { error ->
      copy(
        home =
          home.copy(
            boards = error.toLoadableError(),
            activeTopics = error.toLoadableError(),
          ),
      )
    },
  )

private fun MainContentUiState.withBoardsResult(result: Result<BoardsReadData>): MainContentUiState =
  result.fold(
    onSuccess = { data ->
      copy(
        boards =
          boards.copy(
            subscribedBoards = LoadableUiState.Content(data.subscribedBoards.map { it.toPreview() }),
            categories = LoadableUiState.Content(data.remoteCategories.map { it.toPreview() }),
          ),
      )
    },
    onFailure = { error ->
      copy(
        boards =
          boards.copy(
            subscribedBoards = error.toLoadableError(),
            categories = error.toLoadableError(),
          ),
      )
    },
  )

private fun MainContentUiState.withMessagesResult(result: Result<MessagesReadData>): MainContentUiState =
  result.fold(
    onSuccess = { data ->
      copy(
        messages =
          messages.copy(
            messages = LoadableUiState.Content(data.messages.map { it.toPreview() }),
          ),
      )
    },
    onFailure = { error ->
      copy(
        messages =
          messages.copy(
            messages = error.toLoadableError(),
          ),
      )
    },
  )

private fun MainContentUiState.withProfileResult(
  session: LoginSessionData,
  result: Result<ProfileReadData>,
): MainContentUiState =
  result.fold(
    onSuccess = { data ->
      copy(
        profile =
          profile.copy(
            session = LoadableUiState.Content(session),
            counters = LoadableUiState.Content(data.counters.toPreviews()),
            notifications = LoadableUiState.Content(data.notifications.map { it.toPreview() }),
          ),
      )
    },
    onFailure = { error ->
      copy(
        profile =
          profile.copy(
            counters = error.toLoadableError(),
            notifications = error.toLoadableError(),
          ),
      )
    },
  )

private fun Throwable.toLoadableError(): LoadableUiState<Nothing> =
  if (this is LoginRequiredException) {
    LoadableUiState.LoginRequired
  } else {
    LoadableUiState.Error(message = message ?: "Unknown error", cause = this)
  }
