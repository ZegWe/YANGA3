package com.yanga.client.ui

import com.yanga.client.data.MessagesReadData

internal fun MainContentUiState.withMessagesResult(result: Result<MessagesReadData>): MainContentUiState =
  result.fold(
    onSuccess = { data ->
      copy(messages = messages.copy(messages = LoadableUiState.Content(data.messages.map { it.toPreview() })))
    },
    onFailure = { error ->
      copy(messages = messages.copy(messages = error.toLoadableError()))
    },
  )
