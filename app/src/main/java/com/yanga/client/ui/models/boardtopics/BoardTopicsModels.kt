package com.yanga.client.ui

data class BoardTopicListUiState(
  val boardName: String = "",
  val fid: String = "",
  val iconUrl: String? = null,
  val category: String = "",
  val isFavorite: Boolean = false,
  val topics: LoadableUiState<List<TopicPreview>> = LoadableUiState.Loading,
)

data class BoardDestination(
  val id: String,
  val name: String,
  val iconUrl: String? = null,
  val category: String = "",
  val isFavorite: Boolean = false,
)
