package com.yanga.client.ui.main

import com.yanga.client.api.NgaBoardCategory
import com.yanga.client.api.NgaMessageSummary
import com.yanga.client.api.NgaNotificationSummary
import com.yanga.client.api.NgaProfileCounters
import com.yanga.client.api.NgaTopicSummary

internal fun NgaTopicSummary.toPreview(): TopicPreview =
  TopicPreview(
    title = title,
    board = boardName,
    replies = "$replyCount replies",
    lastActive = lastPostAt?.toString() ?: "",
    authorInitial = authorName.initialOrFallback(),
  )

internal fun NgaBoardCategory.toPreview(): BoardPreview {
  val unreadCount = boards.sumOf { it.unreadCount ?: 0 }
  return BoardPreview(
    name = name,
    metadata = "${boards.size} boards",
    marker = name.initialOrFallback(),
    badge = unreadCount.takeIf { it > 0 }?.toString(),
  )
}

internal fun NgaMessageSummary.toPreview(): MessagePreview =
  MessagePreview(
    contact = contactName,
    preview = listOfNotNull(subject?.takeIf { it.isNotBlank() }, preview.takeIf { it.isNotBlank() })
      .joinToString(separator = " - "),
    time = lastUpdatedAt?.toString() ?: "",
    badge = unreadCount.takeIf { it > 0 }?.toString(),
  )

internal fun NgaNotificationSummary.toPreview(): SettingsPreview =
  SettingsPreview(
    icon = title.initialOrFallback(),
    title = title,
    subtitle = preview,
    badge = unreadCount.takeIf { it > 0 }?.toString(),
  )

internal fun NgaProfileCounters.toPreviews(): List<SettingsPreview> =
  listOf(
    SettingsPreview("星", "Favorite topics", favoriteTopics.toString()),
    SettingsPreview("版", "Subscribed boards", subscribedBoards.toString()),
    SettingsPreview(
      icon = "通",
      title = "Unread notifications",
      subtitle = unreadNotifications.toString(),
      badge = unreadNotifications.takeIf { it > 0 }?.toString(),
    ),
    SettingsPreview(
      icon = "信",
      title = "Unread messages",
      subtitle = unreadMessages.toString(),
      badge = unreadMessages.takeIf { it > 0 }?.toString(),
    ),
  )

private fun String?.initialOrFallback(): String =
  this
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.take(1)
    ?: "#"
