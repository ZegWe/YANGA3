package com.yanga.client.ui

import com.yanga.client.data.BoardsReadData

internal fun BoardsReadData.toBoardsUiState(): BoardsUiState {
  val subscribedBoardPreviews = subscribedBoards.map { it.toPreview().copy(isFavorite = true) }
  val favoriteIds = subscribedBoards.map { it.boardId }.toSet()
  return BoardsUiState(
    subscribedBoards = LoadableUiState.Content(subscribedBoardPreviews),
    sections = LoadableUiState.Content(remoteSections.map { it.toPreview() }.markFavoriteBoards(favoriteIds)),
  )
}

internal fun collectBoardIconUrls(boardsState: BoardsUiState): List<String> =
  buildList {
    ((boardsState.subscribedBoards as? LoadableUiState.Content)?.value ?: emptyList())
      .mapNotNullTo(this) { it.iconUrl?.takeIf(String::isNotBlank) }

    ((boardsState.sections as? LoadableUiState.Content)?.value ?: emptyList())
      .flatMap { it.groups }
      .flatMap { it.boards }
      .mapNotNullTo(this) { it.iconUrl?.takeIf(String::isNotBlank) }
  }
    .distinct()
