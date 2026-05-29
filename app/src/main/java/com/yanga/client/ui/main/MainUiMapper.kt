package com.yanga.client.ui.main

import com.yanga.client.api.NgaBoardCategory
import com.yanga.client.api.NgaBoardGroup
import com.yanga.client.api.NgaBoardSection
import com.yanga.client.api.NgaBoardSummary
import com.yanga.client.api.NgaMessageSummary
import com.yanga.client.api.NgaNotificationSummary
import com.yanga.client.api.NgaProfileCounters
import com.yanga.client.api.NgaThreadPost
import com.yanga.client.api.NgaTopicSummary

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun NgaTopicSummary.toPreview(): TopicPreview =
  TopicPreview(
    id = topicId,
    title = title,
    board = boardName,
    replies = "$replyCount replies",
    lastActive = lastPostAt?.toDateTimeString() ?: "",
    authorInitial = authorName.initialOrFallback(),
  )

internal fun NgaBoardSummary.toPreview(): BoardPreview =
  BoardPreview(
    id = boardId,
    name = name,
    metadata = description ?: "fid: $boardId",
    marker = name.initialOrFallback(),
    badge = unreadCount.takeIf { it != null && it > 0 }?.toString(),
    iconUrl = iconUrl,
  )

internal fun NgaBoardGroup.toPreview(): BoardGroupPreview =
  BoardGroupPreview(
    id = id,
    name = name,
    boards = boards.map { it.toPreview(category = name) },
  )

internal fun NgaBoardSection.toPreview(): BoardSectionPreview =
  BoardSectionPreview(
    id = id,
    name = name,
    groups = groups.map { group -> group.toPreview(categoryPrefix = name) },
  )

internal fun NgaBoardSummary.toPreview(category: String): BoardPreview =
  toPreview().copy(category = category)

internal fun NgaBoardGroup.toPreview(categoryPrefix: String): BoardGroupPreview =
  BoardGroupPreview(
    id = id,
    name = name,
    boards = boards.map { it.toPreview(category = "$categoryPrefix / $name") },
  )

internal fun NgaBoardCategory.toPreview(): BoardPreview {
  val unreadCount = boards.sumOf { it.unreadCount ?: 0 }
  return BoardPreview(
    id = id,
    name = name,
    metadata = "${boards.size} boards",
    marker = name.initialOrFallback(),
    badge = unreadCount.takeIf { it > 0 }?.toString(),
  )
}

internal fun NgaThreadPost.toPreview(): PostPreview =
  PostPreview(
    author = author,
    floor = if (lou == 0) "楼主" else "$lou 楼",
    time = postDate.toDateTimeString(),
    content = content,
    avatarInitial = author.initialOrFallback(),
  )

private fun Long.toDateTimeString(): String {
  val date = Date(if (this < 10000000000L) this * 1000 else this)
  return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(date)
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
