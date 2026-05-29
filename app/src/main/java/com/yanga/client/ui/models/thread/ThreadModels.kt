package com.yanga.client.ui

data class PostPreview(
  val author: String,
  val floor: String,
  val time: String,
  val content: String,
  val avatarInitial: String,
)

data class ThreadUiState(
  val title: String = "",
  val page: String = "1",
  val replyCount: String = "0",
  val posts: LoadableUiState<List<PostPreview>> = LoadableUiState.Loading,
)

data class ThreadDestination(
  val id: String,
  val title: String,
)
