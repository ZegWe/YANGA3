package com.yanga.client.ui.content

import com.yanga.client.data.image.ImageUrlResolver

sealed class PostContentPart {
  data class Text(val text: String) : PostContentPart()

  data class Quote(val text: String) : PostContentPart()

  data class Image(val url: String) : PostContentPart()
}

object PostContentParser {
  private val quoteRegex = Regex("\\[quote\\]([\\s\\S]*?)\\[/quote\\]", RegexOption.IGNORE_CASE)
  private val bbCodeImageRegex = Regex("\\[img(?:\\s+[^\\]]*)?\\]([\\s\\S]*?)\\[/img\\]", RegexOption.IGNORE_CASE)
  private val bbCodeImageAttrRegex = Regex("\\[img\\s+[^\\]]*src\\s*=\\s*[\"']?([^\"'\\]]+)[\"']?[^\\]]*\\]", RegexOption.IGNORE_CASE)
  private val htmlImageRegex = Regex("<img\\s+[^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>", RegexOption.IGNORE_CASE)
  private val relativeImageRegex = Regex("(\\.\\/mon_[^\\s\\]\"'<>]+)")
  private val tokenRegex =
    Regex(
      "\\[quote\\][\\s\\S]*?\\[/quote\\]|\\[img(?:\\s+[^\\]]*)?\\][\\s\\S]*?\\[/img\\]|\\[img\\s+[^\\]]*\\]|<img\\s+[^>]*src\\s*=\\s*[\"'][^\"']+[\"'][^>]*>|\\.\\/mon_[^\\s\\]\"'<>]+",
      RegexOption.IGNORE_CASE,
    )

  fun parse(content: String): List<PostContentPart> {
    val normalized = decodeBasicEntities(content)
    if (normalized.isBlank()) return emptyList()

    val parts = mutableListOf<PostContentPart>()
    var cursor = 0
    for (match in tokenRegex.findAll(normalized)) {
      if (match.range.first > cursor) {
        appendText(parts, normalized.substring(cursor, match.range.first))
      }
      appendToken(parts, match.value)
      cursor = match.range.last + 1
    }

    if (cursor < normalized.length) {
      appendText(parts, normalized.substring(cursor))
    }

    return if (parts.isEmpty()) listOf(PostContentPart.Text(normalized.trim())) else parts
  }

  private fun appendToken(parts: MutableList<PostContentPart>, token: String) {
    quoteRegex.matchEntire(token)?.let { match ->
      val quote = match.groupValues[1].trim()
      if (quote.isNotEmpty()) parts += PostContentPart.Quote(stripMarkup(quote))
      return
    }

    extractImageUrl(token)?.let { url ->
      parts += PostContentPart.Image(ImageUrlResolver.resolve(url))
      return
    }

    appendText(parts, token)
  }

  private fun appendText(parts: MutableList<PostContentPart>, raw: String) {
    val text = stripMarkup(raw).trim()
    if (text.isNotEmpty()) {
      parts += PostContentPart.Text(text)
    }
  }

  private fun extractImageUrl(token: String): String? {
    bbCodeImageRegex.matchEntire(token)?.let { match ->
      return match.groupValues[1].trim().ifBlank { null }
    }
    bbCodeImageAttrRegex.find(token)?.let { match ->
      return match.groupValues[1].trim().ifBlank { null }
    }
    htmlImageRegex.find(token)?.let { match ->
      return match.groupValues[1].trim().ifBlank { null }
    }
    relativeImageRegex.find(token)?.let { match ->
      return match.groupValues[1].trim().ifBlank { null }
    }
    return null
  }

  private fun stripMarkup(value: String): String =
    value
      .replace("<br/>", "\n")
      .replace("<br />", "\n")
      .replace("<br>", "\n")
      .replace(Regex("<[^>]+>"), "")
      .replace("&nbsp;", " ")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&amp;", "&")
      .trim()

  private fun decodeBasicEntities(content: String): String =
    content
      .replace("<br/>", "\n")
      .replace("<br />", "\n")
      .replace("<br>", "\n")
      .replace("&nbsp;", " ")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&amp;", "&")
}
