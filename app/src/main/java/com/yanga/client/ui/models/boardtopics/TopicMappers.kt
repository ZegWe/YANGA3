package com.yanga.client.ui

import com.yanga.client.api.NgaTopicSummary

internal fun NgaTopicSummary.toPreview(): TopicPreview =
  TopicPreview(
    id = topicId,
    title = title,
    board = boardName,
    replies = "$replyCount replies",
    lastActive = lastPostAt?.toUiDateTimeString() ?: "",
    authorInitial = authorName.initialOrFallback(),
  )
