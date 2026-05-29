package com.yanga.client.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MessagesViewModel(
  private val repository: NgaReadOnlyRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(MessagesUiState())
  val state: StateFlow<MessagesUiState> = _state.asStateFlow()

  fun refresh(session: LoginSessionData?) {
    if (session == null) {
      _state.value = MessagesUiState(messages = LoadableUiState.LoginRequired)
      return
    }
    viewModelScope.launch {
      _state.value = MessagesUiState(messages = LoadableUiState.Loading)
      val result = repository.loadMessages(session)
      _state.update { state ->
        result.fold(
          onSuccess = { data -> state.copy(messages = LoadableUiState.Content(data.messages.map { it.toPreview() })) },
          onFailure = { error -> state.copy(messages = error.toLoadableError()) },
        )
      }
    }
  }
}
