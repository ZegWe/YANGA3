package com.yanga.client.api

import org.json.JSONObject

object NgaAccountParser {
  fun parseNotifications(raw: String): List<NgaNotificationSummary> {
    val root = ngaJsonRoot(raw)
    val data = root.objectValue("data") ?: root
    val notifications = data.objectListValue("notifications", "noti", "notices", "list")
      .ifEmpty { root.objectListValue("notifications", "noti", "notices", "list") }

    return notifications.mapNotNull { notification -> notification.toNotificationSummary() }
  }

  fun parseProfileCounters(raw: String): NgaProfileCounters {
    val root = ngaJsonRoot(raw)
    val data = root.objectValue("data") ?: root
    val counters = data.objectValue("counters", "counter", "counts") ?: data.objectValue("0") ?: data

    return NgaProfileCounters(
      topicCount = counters.nullableIntValue("topic_count", "topicCount", "topics", "topic", "threads", "thread_count"),
      replyCount = counters.nullableIntValue("reply_count", "replyCount", "replies", "reply", "posts", "post_count"),
      favoriteTopics = counters.intValue("favorite_topics", "favoriteTopics", "favor", "favorites"),
      subscribedBoards = counters.intValue("subscribed_boards", "subscribedBoards", "subscribed", "subboards"),
      unreadNotifications = counters.intValue("unread_notifications", "unreadNotifications", "noti", "notifications"),
      unreadMessages = counters.intValue("unread_messages", "unreadMessages", "messages", "message", "newpm"),
    )
  }

  fun parseProfileAvatar(raw: String, uid: String = ""): String? =
    parsePublicUserAvatar(raw, uid)

  fun parsePublicUserAvatar(raw: String, uid: String = ""): String? {
    val root = ngaJsonRoot(raw)
    val data = root.objectValue("data") ?: root
    val profile = data.objectValue("0") ?: data
    val resolvedUid = uid.ifBlank { profile.stringValue("uid") }
    val memberId = profile.nullableStringValue("memberid", "gid", "groupid")
    return NgaAvatarUrls.resolveUserAvatar(profile.nullableStringValue("avatar"), resolvedUid, memberId)
  }

  fun parseProfile(raw: String, uid: String = ""): NgaProfileParseResult =
    NgaProfileParseResult(
      counters = parseProfileCounters(raw),
      avatarUrl = parseProfileAvatar(raw, uid),
    )

  private fun JSONObject.toNotificationSummary(): NgaNotificationSummary? {
    val id = stringValue("id", "nid", "notification_id", "notificationId")
    val title = stringValue("title", "subject")
    if (id.isBlank() && title.isBlank()) return null

    return NgaNotificationSummary(
      id = id,
      title = title,
      preview = stringValue("preview", "content", "body"),
      createdAt = nullableLongValue("time", "created_at", "createdAt", "postdate"),
      unreadCount = intValue("unread", "unread_count", "unreadCount"),
    )
  }
}
