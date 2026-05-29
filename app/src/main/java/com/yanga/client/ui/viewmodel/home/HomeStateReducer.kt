package com.yanga.client.ui

import com.yanga.client.data.HomeReadData

internal fun MainContentUiState.withHomeResult(result: Result<HomeReadData>): MainContentUiState =
  result.fold(
    onSuccess = { data ->
      copy(
        home =
          home.copy(
            boards = if (isLoggedIn) home.boards else LoadableUiState.Content(data.boards.map { it.toPreview() }),
            activeTopics = LoadableUiState.Content(data.activeTopics.map { it.toPreview() }),
          ),
      )
    },
    onFailure = { error ->
      copy(
        home =
          home.copy(
            boards = error.toLoadableError(),
            activeTopics = error.toLoadableError(),
          ),
      )
    },
  )
