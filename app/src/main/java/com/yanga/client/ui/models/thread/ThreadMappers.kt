package com.yanga.client.ui

import com.yanga.client.api.NgaThreadPost

internal fun NgaThreadPost.toPreview(): PostPreview =
  PostPreview(
    author = author,
    authorAvatarUrl = authorAvatarUrl,
    floor = if (lou == 0) "楼主" else "$lou 楼",
    time = postDate.toUiDateTimeString(),
    content = content,
    avatarInitial = author.initialOrFallback(),
    attachments = attachments.map { attachment ->
      PostAttachmentPreview(
        name = attachment.name,
        url = attachment.url,
      )
    },
  )
