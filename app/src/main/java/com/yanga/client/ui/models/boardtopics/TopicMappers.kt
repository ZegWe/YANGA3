package com.yanga.client.ui

import com.yanga.client.api.NgaTopicSummary

internal fun NgaTopicSummary.toPreview(): TopicPreview {
  val displayName = authorName.orEmpty()
  return TopicPreview(
    id = topicId,
    title = title,
    board = boardName,
    replies = "$replyCount replies",
    lastActive = lastPostAt?.toUiDateTimeString() ?: "",
    authorName = displayName,
    authorAvatarUrl = authorAvatarUrl,
    authorInitial = displayName.initialOrFallback(),
  )
}
