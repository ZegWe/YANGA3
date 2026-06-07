package com.yanga.client.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
  private val repository: NgaReadOnlyRepository,
  initialMode: SearchMode = SearchMode.Global,
) : ViewModel() {
  private val _state =
    MutableStateFlow(
      SearchUiState(
        mode = initialMode,
        scope = if (initialMode is SearchMode.BoardScoped) SearchScope.Topics else SearchScope.Boards,
      ),
    )
  val state: StateFlow<SearchUiState> = _state.asStateFlow()
  private var searchJob: Job? = null
  private var activeSession: LoginSessionData? = null

  fun setSession(session: LoginSessionData?) {
    activeSession = session
  }

  fun updateQuery(query: String) {
    _state.update { it.copy(query = query) }
  }

  fun setScope(scope: SearchScope) {
    _state.update { state ->
      if (state.mode is SearchMode.BoardScoped) {
        state
      } else {
        state.copy(scope = scope, results = SearchResultsUiState.Idle)
      }
    }
  }

  fun setSearchContent(enabled: Boolean) {
    _state.update { it.copy(searchContent = enabled) }
  }

  fun setEssenceOnly(enabled: Boolean) {
    _state.update { it.copy(essenceOnly = enabled) }
  }

  fun submitSearch(query: String = _state.value.query) {
    val cleanQuery = query.trim()
    _state.update { it.copy(query = query) }
    searchJob?.cancel()

    if (cleanQuery.isBlank()) {
      _state.update { it.copy(results = SearchResultsUiState.Idle) }
      return
    }

    val snapshot = _state.value
    if (snapshot.isTopicSearch && cleanQuery.length > MAX_TOPIC_QUERY_LENGTH) {
      _state.update {
        it.copy(results = SearchResultsUiState.Error("Topic search is too long. Keep it under $MAX_TOPIC_QUERY_LENGTH characters."))
      }
      return
    }

    _state.update { it.copy(results = SearchResultsUiState.Loading) }
    searchJob =
      viewModelScope.launch {
        when {
          snapshot.mode is SearchMode.BoardScoped ->
            searchTopics(
              query = cleanQuery,
              fid = snapshot.mode.board.id,
              searchContent = snapshot.searchContent,
              recommend = snapshot.essenceOnly,
            )
          snapshot.scope == SearchScope.Topics ->
            searchTopics(
              query = cleanQuery,
              fid = null,
              searchContent = snapshot.searchContent,
              recommend = snapshot.essenceOnly,
            )
          else -> searchBoards(cleanQuery)
        }
      }
  }

  private suspend fun searchBoards(query: String) {
    repository.searchBoards(activeSession, query).fold(
      onSuccess = { boards ->
        val previews = boards.map { it.toPreview() }
        _state.update {
          it.copy(
            results =
              if (previews.isEmpty()) {
                SearchResultsUiState.Empty("No boards found")
              } else {
                SearchResultsUiState.Boards(previews)
              },
          )
        }
      },
      onFailure = { error ->
        _state.update { it.copy(results = SearchResultsUiState.Error(error.message ?: "Search failed")) }
      },
    )
  }

  private suspend fun searchTopics(
    query: String,
    fid: String?,
    searchContent: Boolean,
    recommend: Boolean,
  ) {
    repository
      .searchTopics(
        session = activeSession,
        query = query,
        fid = fid,
        page = 1,
        searchContent = searchContent,
        recommend = recommend,
      )
      .fold(
        onSuccess = { data ->
          val previews = data.topics.map { it.toPreview() }
          _state.update {
            it.copy(
              results =
                if (previews.isEmpty()) {
                  SearchResultsUiState.Empty("No topics found")
                } else {
                  SearchResultsUiState.Topics(previews)
                },
            )
          }
        },
        onFailure = { error ->
          _state.update { it.copy(results = SearchResultsUiState.Error(error.message ?: "Search failed")) }
        },
      )
  }

  private companion object {
    const val MAX_TOPIC_QUERY_LENGTH = 40
  }
}
