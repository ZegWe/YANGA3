package com.yanga.client.data.image

object ImageUrlResolver {
  /** Resolve persisted legacy URLs at the request boundary, including signatures. */
  fun resolveForRequest(raw: String): String {
    val value = raw.trim()
    Regex("""^\.u/(\d+)(.*)$""").matchEntire(value)?.let { match ->
      val uid = match.groupValues[1].toLongOrNull() ?: return value
      // NGA commonui.uid2path2: six base-36 digits paired outside-in.
      val shard = uid.toString(36).padStart(6, '0').takeLast(6)
      return "https://user-file.nga.cn/${shard[0]}${shard[5]}/${shard[1]}${shard[4]}/${shard[2]}${shard[3]}/${match.groupValues[1]}${match.groupValues[2]}"
    }
    val resolved = if (value.startsWith("./")) "https://img.nga.cn/attachments/${value.removePrefix("./")}" else resolve(value)
    val uri = runCatching { java.net.URI(resolved) }.getOrNull() ?: return resolved
    val host = uri.host?.lowercase().orEmpty()
    if (Regex("""img\d?\.nga\.178\.com""").matches(host) && uri.rawPath.orEmpty().startsWith("/attachments/")) {
      return "https://img.nga.cn" + uri.rawPath + (uri.rawQuery?.let { "?$it" } ?: "") + (uri.rawFragment?.let { "#$it" } ?: "")
    }
    return resolved
  }

  fun resolve(raw: String, attachmentBase: String?): String {
    val base = attachmentBase?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() }
      ?: return resolve(raw)
    val path = raw.trim().removePrefix("./").removePrefix("/")
    if (!path.startsWith("mon_") && !raw.trim().startsWith("./")) return resolve(raw)
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
      trimmed.startsWith("./") -> "https://img.nga.178.com/attachments/${trimmed.removePrefix("./")}"
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
