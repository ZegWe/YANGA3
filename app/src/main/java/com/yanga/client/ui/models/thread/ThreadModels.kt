package com.yanga.client.ui

data class PostEmbeddedReplyPreview(
  val author: String,
  val authorAvatarUrl: String? = null,
  val content: String,
)

data class PostPreview(
  val author: String,
  val authorAvatarUrl: String? = null,
  val floor: String,
  val time: String,
  val content: String,
  val avatarInitial: String,
  val embeddedComments: List<PostEmbeddedReplyPreview> = emptyList(),
  val hotReplies: List<PostEmbeddedReplyPreview> = emptyList(),
  val attachments: List<PostAttachmentPreview> = emptyList(),
)

data class PostAttachmentPreview(
  val name: String,
  val url: String,
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
  val page: Int = 1,
)
