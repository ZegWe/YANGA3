package com.yanga.client.ui

import com.yanga.client.api.NgaSubBoard
import com.yanga.client.api.NgaTopicEntryTarget
import com.yanga.client.api.NgaTopicEntryType
import com.yanga.client.api.NgaTopicSummary

internal fun NgaSubBoard.toOption(): SubBoardOption =
  SubBoardOption(
    id = id,
    name = name,
    valueId = valueId,
    subscribeId = subscribeId,
  )

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
    navigationTarget = entryTarget.toNavigationTarget(title = title, boardName = boardName),
  )
}

private fun NgaTopicEntryTarget?.toNavigationTarget(
  title: String,
  boardName: String,
): TopicNavigationTarget {
  val target = this ?: return TopicNavigationTarget.Thread
  return when (target.type) {
    NgaTopicEntryType.Board ->
      TopicNavigationTarget.Board(
        BoardDestination(
          id = target.id,
          name = title.ifBlank { boardName },
          category = boardName,
        ),
      )
    NgaTopicEntryType.Collection ->
      TopicNavigationTarget.Board(
        BoardDestination(
          id = target.id,
          name = title.ifBlank { boardName },
          category = boardName,
        ),
      )
  }
}
