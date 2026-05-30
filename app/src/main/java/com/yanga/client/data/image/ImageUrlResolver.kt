package com.yanga.client.data.image

import com.yanga.client.api.NgaStaticUrls

object ImageUrlResolver {
  fun resolve(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return trimmed

    return when {
      trimmed.startsWith("./") -> NgaStaticUrls.expandRelativeImage(trimmed)
      trimmed.startsWith("//") -> "https:$trimmed"
      trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
      else -> trimmed
    }
  }

  fun fileName(url: String): String {
    val path = url.substringBefore('?').substringBefore('#')
    return path.substringAfterLast('/').ifBlank { url.hashCode().toString().replace('-', 'x') }
  }
}
