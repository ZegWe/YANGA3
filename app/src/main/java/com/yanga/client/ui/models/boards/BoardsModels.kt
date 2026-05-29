package com.yanga.client.ui

data class BoardPreview(
  val id: String,
  val name: String,
  val metadata: String,
  val marker: String,
  val badge: String? = null,
  val iconUrl: String? = null,
  val category: String = "",
  val isFavorite: Boolean = false,
)

data class BoardGroupPreview(
  val id: String,
  val name: String,
  val boards: List<BoardPreview>,
)

data class BoardSectionPreview(
  val id: String,
  val name: String,
  val groups: List<BoardGroupPreview>,
)

data class BoardsUiState(
  val subscribedBoards: LoadableUiState<List<BoardPreview>> = LoadableUiState.Loading,
  val sections: LoadableUiState<List<BoardSectionPreview>> = LoadableUiState.Loading,
)

internal val favoriteBoards: List<BoardPreview> = emptyList()
internal val subscribedBoards: List<BoardPreview> = emptyList()
internal val forumCategories: List<BoardPreview> = emptyList()
