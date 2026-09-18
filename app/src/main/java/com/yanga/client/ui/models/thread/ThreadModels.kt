package com.yanga.client.ui

data class PostEmbeddedReplyPreview(
  val authorId: String = "",
  val pid: String = "",
  val tid: String = "",
  val floorNumber: Int = 0,
  val author: String,
  val authorAvatarUrl: String? = null,
  val content: String,
)

data class PostPreview(
  val pid: String = "",
  val floorNumber: Int = 0,
  val author: String,
  val authorAvatarUrl: String? = null,
  val floor: String,
  val time: String,
  val content: String,
  val avatarInitial: String,
  val embeddedComments: List<PostEmbeddedReplyPreview> = emptyList(),
  val hotReplies: List<PostEmbeddedReplyPreview> = emptyList(),
  val attachments: List<PostAttachmentPreview> = emptyList(),
  val poll: com.yanga.client.api.NgaPoll? = null,
  val authorId: String = "",
  val isOriginalPoster: Boolean = false,
  val bodyColor: String? = null,
  val score: Int = 0,
)

data class PostAttachmentPreview(
  val name: String,
  val url: String,
)

data class ThreadUiState(
  val isRefreshing: Boolean = false,
  val refreshError: String? = null,
  val title: String = "",
  val page: String = "1",
  val maxPage: String = "1",
  val replyCount: String = "0",
  val targetPostId: String? = null,
  val targetFloorNumber: Int? = null,
  val targetScrollRequestId: Int = 0,
  val filteredAuthorId: String? = null,
  val filteredAuthorName: String? = null,
  val cachedPostsByPage: Map<Int, List<PostPreview>> = emptyMap(),
  val posts: LoadableUiState<List<PostPreview>> = LoadableUiState.Loading,
)

@kotlinx.serialization.Serializable
data class ThreadDestination(
  val id: String,
  val title: String,
  val page: Int = 1,
  val targetPostId: String? = null,
  val targetFloorNumber: Int? = null,
)
