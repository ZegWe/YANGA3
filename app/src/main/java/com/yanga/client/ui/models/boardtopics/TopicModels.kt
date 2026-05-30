package com.yanga.client.ui

data class TopicPreview(
  val id: String,
  val title: String,
  val board: String,
  val replies: String,
  val lastActive: String,
  val authorName: String = "",
  val authorAvatarUrl: String? = null,
  val authorInitial: String,
)

data class HomeUiState(
  val boards: LoadableUiState<List<BoardPreview>> = LoadableUiState.Loading,
  val activeTopics: LoadableUiState<List<TopicPreview>> = LoadableUiState.Loading,
)

internal val activeTopics: List<TopicPreview> = emptyList()
