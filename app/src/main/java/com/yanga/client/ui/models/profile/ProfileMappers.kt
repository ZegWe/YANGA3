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
    SettingsPreview("topic", "主题", topicCount?.toString() ?: "--"),
    SettingsPreview("reply", "回复", replyCount?.toString() ?: "--"),
    SettingsPreview(
      icon = "notification",
      title = "通知",
      subtitle = unreadNotifications.toString(),
      badge = unreadNotifications.takeIf { it > 0 }?.toString(),
    ),
  )
