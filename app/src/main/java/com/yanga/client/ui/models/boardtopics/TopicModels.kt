package com.yanga.client.ui

data class TopicPreview(
  val id: String,
  val title: String,
  val board: String,
  val replyCount: Int,
  val lastActive: String,
  val authorName: String = "",
  val authorId: String = "",
  val navigationTarget: TopicNavigationTarget = TopicNavigationTarget.Thread,
)

sealed interface TopicNavigationTarget {
  data object Thread : TopicNavigationTarget
  data class Board(val destination: BoardDestination) : TopicNavigationTarget
}

data class HomeUiState(
  val boards: LoadableUiState<List<BoardPreview>> = LoadableUiState.Loading,
  val activeTopics: LoadableUiState<List<TopicPreview>> = LoadableUiState.Loading,
)

internal val activeTopics: List<TopicPreview> = emptyList()
