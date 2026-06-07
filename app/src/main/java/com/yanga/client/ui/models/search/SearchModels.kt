package com.yanga.client.ui

data class SearchRoute(
  val mode: SearchMode,
)

sealed interface SearchMode {
  data object Global : SearchMode
  data class BoardScoped(val board: BoardDestination) : SearchMode
}

enum class SearchScope {
  Boards,
  Topics,
}

data class SearchUiState(
  val mode: SearchMode = SearchMode.Global,
  val query: String = "",
  val scope: SearchScope = SearchScope.Boards,
  val searchContent: Boolean = false,
  val essenceOnly: Boolean = false,
  val results: SearchResultsUiState = SearchResultsUiState.Idle,
) {
  val isTopicSearch: Boolean
    get() = mode is SearchMode.BoardScoped || scope == SearchScope.Topics
}

sealed interface SearchResultsUiState {
  data object Idle : SearchResultsUiState
  data object Loading : SearchResultsUiState
  data class Empty(val message: String) : SearchResultsUiState
  data class Error(val message: String) : SearchResultsUiState
  data class Boards(val boards: List<BoardPreview>) : SearchResultsUiState
  data class Topics(val topics: List<TopicPreview>) : SearchResultsUiState
}
