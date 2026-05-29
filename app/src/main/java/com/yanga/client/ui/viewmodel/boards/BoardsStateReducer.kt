package com.yanga.client.ui

import com.yanga.client.data.BoardsReadData
import com.yanga.client.data.LocalFavoriteBoard

internal fun MainContentUiState.withBoardsResult(result: Result<BoardsReadData>): MainContentUiState =
  result.fold(
    onSuccess = { data ->
      val subscribedBoardPreviews = data.subscribedBoards.map { it.toPreview() }
      val favoriteIds = data.subscribedBoards.map { it.boardId }.toSet()
      copy(
        home = if (isLoggedIn) {
          home.copy(boards = LoadableUiState.Content(subscribedBoardPreviews.map { it.copy(isFavorite = true) }))
        } else {
          home
        },
        boards =
          boards.copy(
            subscribedBoards = LoadableUiState.Content(subscribedBoardPreviews.map { it.copy(isFavorite = true) }),
            sections = LoadableUiState.Content(data.remoteSections.map { it.toPreview() }.markFavoriteBoards(favoriteIds)),
          ),
      )
    },
    onFailure = { error ->
      copy(
        boards =
          boards.copy(
            subscribedBoards = error.toLoadableError(),
            sections = error.toLoadableError(),
          ),
      )
    },
  )

internal fun LoadableUiState<List<BoardPreview>>.withFavorites(favoriteIds: Set<String>): LoadableUiState<List<BoardPreview>> =
  when (this) {
    is LoadableUiState.Content -> LoadableUiState.Content(value.markFavorites(favoriteIds))
    else -> this
  }

internal fun LoadableUiState<List<BoardSectionPreview>>.withFavoriteSections(favoriteIds: Set<String>): LoadableUiState<List<BoardSectionPreview>> =
  when (this) {
    is LoadableUiState.Content -> LoadableUiState.Content(value.markFavoriteBoards(favoriteIds))
    else -> this
  }

internal fun List<BoardPreview>.markFavorites(favoriteIds: Set<String>): List<BoardPreview> =
  map { it.copy(isFavorite = favoriteIds.contains(it.id)) }

internal fun List<BoardSectionPreview>.markFavoriteBoards(favoriteIds: Set<String>): List<BoardSectionPreview> =
  map { section ->
    section.copy(
      groups = section.groups.map { group ->
        group.copy(boards = group.boards.markFavorites(favoriteIds))
      },
    )
  }

internal fun LocalFavoriteBoard.toBoardPreview(): BoardPreview =
  BoardPreview(
    id = boardId,
    name = name,
    metadata = "fid: $boardId",
    marker = name.take(1).ifBlank { "B" },
    iconUrl = iconUrl,
    category = category,
    isFavorite = true,
  )
