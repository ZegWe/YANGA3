package com.yanga.client.ui

import com.yanga.client.api.NgaNotificationSummary
import com.yanga.client.api.NgaProfileCounters

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
