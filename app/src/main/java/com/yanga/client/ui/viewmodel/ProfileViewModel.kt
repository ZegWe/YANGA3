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

  fun setEndpoint(endpoint: String) {
    _state.update { it.copy(forumEndpoint = endpoint) }
  }

  fun refresh(session: LoginSessionData?) {
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
      _state.update { current ->
        result.fold(
          onSuccess = { data ->
            current.copy(
              session = LoadableUiState.Content(session),
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

