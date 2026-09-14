package com.yanga.client.ui

import com.yanga.client.api.NgaThreadEmbeddedReply
import com.yanga.client.api.NgaThreadPost

internal fun NgaThreadPost.toPreview(): PostPreview =
  PostPreview(
    pid = pid,
    floorNumber = lou,
    author = author,
    authorAvatarUrl = authorAvatarUrl,
    floor = "$lou 楼",
    authorId = authorId,
    isOriginalPoster = isOriginalPoster,
    score = score,
    time = postDate.toUiDateTimeString(),
    content = content,
    poll = poll,
    avatarInitial = author.initialOrFallback(),
    embeddedComments = embeddedComments.map { it.toPreview() },
    hotReplies = hotReplies.map { it.toPreview() },
    attachments = attachments.map { attachment ->
      PostAttachmentPreview(
        name = attachment.name,
        url = attachment.url,
      )
    },
  )

private fun NgaThreadEmbeddedReply.toPreview(): PostEmbeddedReplyPreview =
  PostEmbeddedReplyPreview(
    authorId = authorId,
    pid = pid,
    tid = tid,
    floorNumber = lou,
    author = author,
    authorAvatarUrl = authorAvatarUrl,
    content = content,
  )
