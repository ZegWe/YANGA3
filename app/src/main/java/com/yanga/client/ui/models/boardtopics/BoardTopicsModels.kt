package com.yanga.client.ui

data class SubBoardOption(
  val id: String,
  val name: String,
  val valueId: String = id.removePrefix("t"),
  val subscribeId: String? = null,
)

enum class BoardTopicFilter {
  All,
  Recommend,
}

data class BoardTopicListUiState(
  val boardName: String = "",
  val fid: String = "",
  val iconUrl: String? = null,
  val category: String = "",
  val isFavorite: Boolean = false,
  val selectedTopicFilter: BoardTopicFilter = BoardTopicFilter.All,
  val subBoards: LoadableUiState<List<SubBoardOption>> = LoadableUiState.Loading,
  val selectedSubBoardIds: Set<String> = emptySet(),
  val topics: LoadableUiState<List<TopicPreview>> = LoadableUiState.Loading,
  val isRefreshing: Boolean = false,
  val currentTopicPage: Int = 1,
  val hasNextTopicPage: Boolean = false,
  val isLoadingNextTopicPage: Boolean = false,
)

@kotlinx.serialization.Serializable
data class BoardDestination(
  val id: String,
  val name: String,
  val iconUrl: String? = null,
  val category: String = "",
  val isFavorite: Boolean = false,
)
