package com.yanga.client.data.image

import com.yanga.client.api.NgaStaticUrls

object ImageUrlResolver {
  fun resolve(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return trimmed

    return when {
      trimmed.startsWith("./") -> NgaStaticUrls.expandRelativeImage(trimmed)
      trimmed.startsWith("//img6.nga.178.com/attachments/") ->
        "https://img.nga.178.com/attachments/${trimmed.substringAfter("/attachments/")}"
      trimmed.startsWith("//") -> "https:$trimmed"
      trimmed.startsWith("http://") || trimmed.startsWith("https://") -> normalizeNgaAttachmentHost(trimmed)
      else -> trimmed
    }
  }

  private fun normalizeNgaAttachmentHost(url: String): String =
    if (
      url.startsWith("http://img6.nga.178.com/attachments/") ||
        url.startsWith("https://img6.nga.178.com/attachments/")
    ) {
      "https://img.nga.178.com/attachments/${url.substringAfter("/attachments/")}"
    } else {
      url
    }

  fun fileName(url: String): String {
    val path = url.substringBefore('?').substringBefore('#')
    return path.substringAfterLast('/').ifBlank { url.hashCode().toString().replace('-', 'x') }
  }
}
