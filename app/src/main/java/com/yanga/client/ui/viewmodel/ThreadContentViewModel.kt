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

class ThreadContentViewModel(
  private val repository: NgaReadOnlyRepository,
) : ViewModel() {
  private val _state = MutableStateFlow<ThreadUiState?>(null)
  val state: StateFlow<ThreadUiState?> = _state.asStateFlow()
  private var loadingThreadId: String? = null
  private var activeThreadId: String? = null

  fun matchesThread(threadId: String): Boolean = activeThreadId == threadId

  fun openThread(session: LoginSessionData?, topic: TopicPreview) {
    openThread(session = session, destination = ThreadDestination(id = topic.id, title = topic.title))
  }

  fun openThread(session: LoginSessionData?, destination: ThreadDestination) {
    val threadId = destination.id
    val fallbackTitle = destination.title
    val cached = _state.value
    if (activeThreadId == threadId && cached?.posts is LoadableUiState.Content) {
      loadingThreadId = threadId
      return
    }
    loadingThreadId = threadId
    activeThreadId = threadId
    _state.value = ThreadUiState(title = fallbackTitle)
    viewModelScope.launch {
      val result = repository.loadThread(session, threadId)
      _state.update { current ->
        if (loadingThreadId == threadId && current != null) {
          result.fold(
            onSuccess = { data ->
              ThreadUiState(
                title = data.subject.ifBlank { fallbackTitle },
                page = data.page.toString(),
                replyCount = data.posts.size.toString(),
                posts = LoadableUiState.Content(data.posts.map { p -> p.toPreview() }),
              )
            },
            onFailure = { ThreadUiState(title = fallbackTitle, posts = it.toLoadableError()) },
          )
        } else {
          current
        }
      }
    }
  }

  fun backFromThread() {
    loadingThreadId = null
    activeThreadId = null
    _state.value = null
  }
}

