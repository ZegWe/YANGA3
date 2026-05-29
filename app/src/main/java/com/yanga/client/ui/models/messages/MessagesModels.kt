package com.yanga.client.ui

data class MessagePreview(
  val contact: String,
  val preview: String,
  val time: String,
  val badge: String? = null,
)

data class MessagesUiState(
  val messages: LoadableUiState<List<MessagePreview>> = LoadableUiState.LoginRequired,
)

internal val privateMessages: List<MessagePreview> = emptyList()
