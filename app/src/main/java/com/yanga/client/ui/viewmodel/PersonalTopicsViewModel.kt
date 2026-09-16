package com.yanga.client.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yanga.client.api.NgaPersonalTopicKind
import com.yanga.client.api.NgaPersonalTopic
import com.yanga.client.api.NgaPersonalTopicPage
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class PersonalTopicsUiState(
  val items: List<NgaPersonalTopic> = emptyList(),
  val page: Int = 0,
  val hasNextPage: Boolean = false,
  val isLoading: Boolean = false,
  val isRefreshing: Boolean = false,
  val isLoadingNext: Boolean = false,
  val error: String? = null,
  val nextPageError: String? = null,
)

internal class PersonalTopicsViewModel(private val repository: NgaReadOnlyRepository) : ViewModel() {
  private data class Request(
    val kind: NgaPersonalTopicKind,
    val session: LoginSessionData?,
    val authorUid: String?,
  )

  private var request: Request? = null
  private var job: Job? = null
  private var generation = 0
  private val _state = MutableStateFlow(PersonalTopicsUiState())
  val state = _state.asStateFlow()

  // Navigation keeps this ViewModel alive while the list's composition is disposed.
  fun ensureLoaded(kind: NgaPersonalTopicKind, session: LoginSessionData?, authorUid: String?) {
    val next = Request(kind, session, authorUid)
    if (request == next) return
    job?.cancel()
    generation++
    request = next
    _state.value = PersonalTopicsUiState()
    refresh()
  }

  fun refresh() {
    val currentRequest = request ?: return
    if (currentRequest.session == null && currentRequest.authorUid == null) return
    if (_state.value.isLoading || _state.value.isRefreshing) return
    job?.cancel()
    val currentGeneration = ++generation
    val current = _state.value
    _state.value = current.copy(
      isLoading = current.page == 0,
      isRefreshing = current.page > 0,
      isLoadingNext = false,
      error = null,
      nextPageError = null,
    )
    job = viewModelScope.launch {
      val result = fetch(currentRequest, 1)
      if (generation != currentGeneration) return@launch
      _state.value = result.fold(
        onSuccess = { data -> PersonalTopicsUiState(
          items = data.items.distinctBy { it.tid to it.pid },
          page = 1,
          hasNextPage = data.hasNextPage && data.items.isNotEmpty(),
        ) },
        onFailure = { current.copy(error = it.message ?: "请稍后重试", isLoadingNext = false) },
      )
    }
  }

  fun loadNextPage() {
    val currentRequest = request ?: return
    val current = _state.value
    if (!current.hasNextPage || current.isLoading || current.isRefreshing || current.isLoadingNext) return
    val currentGeneration = generation
    val nextPage = current.page + 1
    _state.value = current.copy(isLoadingNext = true, nextPageError = null)
    job = viewModelScope.launch {
      val result = fetch(currentRequest, nextPage)
      if (generation != currentGeneration) return@launch
      _state.value = result.fold(
        onSuccess = { data ->
          val merged = (current.items + data.items).distinctBy { it.tid to it.pid }
          current.copy(
            items = merged,
            page = nextPage,
            hasNextPage = data.hasNextPage && merged.size > current.items.size,
            isLoadingNext = false,
            nextPageError = null,
          )
        },
        onFailure = { current.copy(isLoadingNext = false, nextPageError = it.message ?: "请稍后重试") },
      )
    }
  }

  private suspend fun fetch(request: Request, page: Int): Result<NgaPersonalTopicPage> =
    with(request) {
      if (authorUid != null) {
        repository.loadUserTopics(session, authorUid, page)
      } else {
        repository.loadPersonalTopics(session, kind, page)
      }
    }
}
