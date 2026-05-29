package com.yanga.client.ui

import com.yanga.client.api.NgaMessageSummary

internal fun NgaMessageSummary.toPreview(): MessagePreview =
  MessagePreview(
    contact = contactName,
    preview = listOfNotNull(subject?.takeIf { it.isNotBlank() }, preview.takeIf { it.isNotBlank() }).joinToString(separator = " - "),
    time = lastUpdatedAt?.toString() ?: "",
    badge = unreadCount.takeIf { it > 0 }?.toString(),
  )
