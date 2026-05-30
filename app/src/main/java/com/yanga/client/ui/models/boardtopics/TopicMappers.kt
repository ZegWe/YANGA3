package com.yanga.client.ui

import com.yanga.client.api.NgaTopicSummary

internal fun NgaTopicSummary.toPreview(): TopicPreview {
  val displayName = authorName.orEmpty()
  return TopicPreview(
    id = topicId,
    title = title,
    board = boardName,
    replyCount = replyCount,
    lastActive = lastPostAt?.toUiDateTimeString() ?: "",
    authorName = displayName,
    authorId = authorId.orEmpty(),
  )
}
