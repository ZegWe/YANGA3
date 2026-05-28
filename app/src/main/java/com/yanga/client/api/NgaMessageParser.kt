package com.yanga.client.api

import org.json.JSONObject

object NgaMessageParser {
  fun parseList(raw: String): List<NgaMessageSummary> {
    val root = ngaJsonRoot(raw)
    val data = root.objectValue("data") ?: root
    val messages = data.objectListValue("__M", "message", "messages", "list")
      .ifEmpty { root.objectListValue("__M", "message", "messages", "list") }

    return messages.mapNotNull { message -> message.toMessageSummary() }
  }

  private fun JSONObject.toMessageSummary(): NgaMessageSummary? {
    val messageId = stringValue("mid", "message_id", "messageId", "id")
    val contactName = stringValue("contact_name", "from_username", "to_username", "username", "name", "author")
    if (messageId.isBlank() && contactName.isBlank()) return null

    return NgaMessageSummary(
      messageId = messageId,
      contactId = nullableStringValue("uid", "contact_id", "contactId", "from_uid", "to_uid"),
      contactName = contactName,
      subject = nullableStringValue("subject", "title"),
      preview = stringValue("preview", "content", "body", "latest"),
      lastUpdatedAt = nullableLongValue("time", "last_updated_at", "lastUpdatedAt", "postdate"),
      unreadCount = intValue("unread", "unread_count", "unreadCount"),
    )
  }
}
