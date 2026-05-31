package com.yanga.client.api

data class NgaTopicSummary(
  val topicId: String,
  val boardId: String,
  val boardName: String,
  val title: String,
  val authorId: String? = null,
  val authorName: String? = null,
  val authorAvatarUrl: String? = null,
  val replyCount: Int = 0,
  val lastPostAt: Long? = null,
  val isFavorited: Boolean = false,
  val subForumFid: String? = null,
  val categoryTopicId: String? = null,
)

data class NgaSubBoard(
  val id: String,
  val name: String,
  val valueId: String = id.removePrefix("t"),
  val subscribeId: String? = null,
)

data class NgaTopicList(
  val topics: List<NgaTopicSummary>,
  val page: Int,
  val hasNextPage: Boolean,
  val subBoards: List<NgaSubBoard> = emptyList(),
  val boardFid: String = "",
)

data class NgaBoardSummary(
  val boardId: String,
  val name: String,
  val description: String? = null,
  val todayTopicCount: Int? = null,
  val unreadCount: Int? = null,
  val isSubscribed: Boolean = false,
  val iconUrl: String? = null,
)

data class NgaBoardCategory(
  val id: String,
  val name: String,
  val boards: List<NgaBoardSummary>,
)

data class NgaBoardGroup(
  val id: String,
  val name: String,
  val boards: List<NgaBoardSummary>,
)

data class NgaBoardSection(
  val id: String,
  val name: String,
  val groups: List<NgaBoardGroup>,
)

data class NgaMessageSummary(
  val messageId: String,
  val contactId: String? = null,
  val contactName: String,
  val subject: String? = null,
  val preview: String,
  val lastUpdatedAt: Long? = null,
  val unreadCount: Int = 0,
)

data class NgaNotificationSummary(
  val id: String,
  val title: String,
  val preview: String,
  val createdAt: Long? = null,
  val unreadCount: Int = 0,
)

data class NgaProfileCounters(
  val favoriteTopics: Int = 0,
  val subscribedBoards: Int = 0,
  val unreadNotifications: Int = 0,
  val unreadMessages: Int = 0,
)

data class NgaProfileParseResult(
  val counters: NgaProfileCounters,
  val avatarUrl: String? = null,
)
