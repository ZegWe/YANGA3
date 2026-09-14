package com.yanga.client.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yanga.client.api.NgaDomains
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
  private val repository: NgaReadOnlyRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(ProfileUiState(forumEndpoint = NgaDomains.BBS_NGA_CN))
  val state: StateFlow<ProfileUiState> = _state.asStateFlow()

  private var generation = 0

  fun checkIn(session: LoginSessionData?) {
    if (session == null || _state.value.checkInRunning) return
    val requestGeneration = generation
    _state.update { it.copy(checkInRunning = true, checkInMessage = "签到中…") }
    viewModelScope.launch {
      val result = repository.checkIn(session)
      if (requestGeneration != generation) return@launch
      _state.update { it.copy(checkInRunning = false, checkInMessage = result.getOrElse { error -> error.message ?: "签到失败，请重试" }) }
    }
  }

  fun setEndpoint(endpoint: String) {
    _state.update { it.copy(forumEndpoint = endpoint) }
  }

  fun refresh(session: LoginSessionData?) {
    val requestGeneration = ++generation
    _state.update { it.copy(checkInRunning = false, checkInMessage = null) }
    if (session == null) {
      _state.value =
        ProfileUiState(
          session = LoadableUiState.LoginRequired,
          counters = LoadableUiState.LoginRequired,
          notifications = LoadableUiState.LoginRequired,
          forumEndpoint = _state.value.forumEndpoint,
        )
      return
    }
    viewModelScope.launch {
      _state.update {
        it.copy(
          session = LoadableUiState.Content(session),
          counters = LoadableUiState.Loading,
          notifications = LoadableUiState.Loading,
        )
      }
      val result = repository.loadProfile(session)
      if (requestGeneration != generation) return@launch
      _state.update { current ->
        result.fold(
          onSuccess = { data ->
            current.copy(
              session = LoadableUiState.Content(session.copy(avatarUrl = data.avatarUrl)),
              counters = LoadableUiState.Content(data.counters.toPreviews()),
              notifications = LoadableUiState.Content(data.notifications.map { it.toPreview() }),
            )
          },
          onFailure = { error ->
            current.copy(
              counters = error.toLoadableError(),
              notifications = error.toLoadableError(),
            )
          },
        )
      }
    }
  }
}

