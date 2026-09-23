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
  private var checkInGeneration = 0
  private var statusGeneration = 0
  private var checkInSession: LoginSessionData? = null

  private fun setCheckInSession(session: LoginSessionData?) {
    if (checkInSession == session) return
    checkInSession = session
    checkInGeneration++
    statusGeneration++
    _state.update { it.copy(checkedIn = null, checkInRunning = false, checkInStatusLoading = false, checkInStatusError = null, checkInMessage = null) }
  }

  fun refreshCheckInState(session: LoginSessionData?) {
    setCheckInSession(session)
    if (session == null || _state.value.checkInRunning) return
    val requestGeneration = ++statusGeneration
    _state.update { it.copy(checkedIn = null, checkInStatusLoading = true, checkInStatusError = null) }
    viewModelScope.launch {
      val result = repository.loadCheckInStatus(session)
      if (requestGeneration != statusGeneration) return@launch
      _state.update { it.copy(
        checkedIn = result.getOrNull(),
        checkInStatusLoading = false,
        checkInStatusError = if (result.isFailure) "签到状态查询失败，请刷新重试" else null,
      ) }
    }
  }

  fun checkIn(session: LoginSessionData?) {
    setCheckInSession(session)
    if (session == null || _state.value.checkInRunning) return
    val requestGeneration = ++checkInGeneration
    ++statusGeneration // A query started before this action must not overwrite its subsequent query.
    _state.update { it.copy(checkedIn = null, checkInRunning = true, checkInStatusLoading = false, checkInStatusError = null, checkInMessage = "签到中…") }
    viewModelScope.launch {
      val result = repository.checkIn(session)
      if (requestGeneration != checkInGeneration) return@launch
      _state.update { it.copy(checkInRunning = false, checkInMessage = result.getOrElse { error -> error.message ?: "签到失败，请重试" }) }
      refreshCheckInState(session)
    }
  }

  fun setEndpoint(endpoint: String) {
    if (endpoint != _state.value.forumEndpoint) {
      checkInGeneration++
      statusGeneration++
      _state.update { it.copy(checkedIn = null, checkInRunning = false, checkInStatusLoading = false, checkInStatusError = null, checkInMessage = null) }
    }
    _state.update { it.copy(forumEndpoint = endpoint) }
  }

  fun refresh(session: LoginSessionData?) {
    refreshCheckInState(session)
    val requestGeneration = ++generation
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

