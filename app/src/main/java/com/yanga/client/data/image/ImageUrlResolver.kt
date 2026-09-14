package com.yanga.client.data.image

import com.yanga.client.api.NgaStaticUrls

object ImageUrlResolver {
  fun resolve(raw: String, attachmentBase: String?): String {
    val base = attachmentBase?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() }
      ?: return resolve(raw)
    val path = raw.trim().removePrefix("./").removePrefix("/")
    if (!path.startsWith("mon_")) return resolve(raw)
    val absoluteBase = when {
      base.startsWith("//") -> "https:$base"
      base.startsWith("https://") || base.startsWith("http://") -> base
      else -> "https://$base"
    }
    return "$absoluteBase/$path"
  }

  fun resolve(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return trimmed

    // Persisted board directories and post bodies may still contain the legacy image host.
    val legacyIconPath = trimmed
      .removePrefix("https:")
      .removePrefix("http:")
      .takeIf { it.startsWith("//img4.nga.178.com/") }
      ?.removePrefix("//img4.nga.178.com")
    if (legacyIconPath != null && (
        legacyIconPath.startsWith("/ngabbs/nga_classic/f/app/") ||
          legacyIconPath.startsWith("/proxy/cache_attach/ficon/") ||
          legacyIconPath.startsWith("/ngabbs/post/smile/")
      )) {
      return "https://img4.nga.cn$legacyIconPath"
    }

    return when {
      trimmed.startsWith("./") -> NgaStaticUrls.expandRelativeImage(trimmed)
      trimmed.startsWith("/mon_") -> "https://img.nga.178.com/attachments${trimmed}"
      trimmed.startsWith("mon_") -> "https://img.nga.178.com/attachments/$trimmed"
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
