package com.yanga.client.ui.content

import com.yanga.client.api.NgaStaticUrls
import com.yanga.client.data.image.ImageUrlResolver

data class PostTextStyleRange(
  val start: Int,
  val end: Int,
  val bold: Boolean = false,
  val italic: Boolean = false,
  val underline: Boolean = false,
  val strikeThrough: Boolean = false,
  val color: String? = null,
  val sizePercent: Int? = null,
  val linkUrl: String? = null,
)

sealed class PostContentPart {
  data class ListBlock(val items: List<List<PostContentPart>>, val marker: String? = null) : PostContentPart()
  data class Collapse(val title: String, val parts: List<PostContentPart>) : PostContentPart()
  data class Code(val text: String, val language: String = "") : PostContentPart()
  data class Heading(val parts: List<PostContentPart>) : PostContentPart()
  data class CellSpan(val columns: Int = 1, val rows: Int = 1)
  data class Table(
    val rows: List<List<List<PostContentPart>>>,
    val spans: List<List<CellSpan>> = emptyList(),
  ) : PostContentPart()
  data object Rule : PostContentPart()
  data class Text(
    val text: String,
    val styles: List<PostTextStyleRange> = emptyList(),
  ) : PostContentPart()

  data class Quote(
    val parts: List<PostContentPart>,
  ) : PostContentPart() {
    constructor(
      text: String,
      styles: List<PostTextStyleRange> = emptyList(),
    ) : this(
      parts =
        if (text.isEmpty()) {
          emptyList()
        } else {
          listOf(PostContentPart.Text(text, styles))
        },
    )
  }

  data class Image(val url: String) : PostContentPart()

  data class Emoticon(
    val code: String,
    val url: String,
    val alt: String,
  ) : PostContentPart()

  data class Audio(
    val url: String,
    val label: String,
  ) : PostContentPart()
}

object PostContentParser {
  fun parse(content: String): List<PostContentPart> {
    val normalized = ContentNormalizer.prepare(content)
    if (normalized.isBlank()) return emptyList()

    val parts = BbContentParser(normalized).parseDocument()
    return parts.ifEmpty { listOf(PostContentPart.Text(normalized.trim())) }
  }

  fun collectImageUrls(parts: List<PostContentPart>): List<String> =
    buildList {
      for (part in parts) {
        when (part) {
          is PostContentPart.Image -> add(part.url)
          is PostContentPart.Quote -> addAll(collectImageUrls(part.parts))
          is PostContentPart.ListBlock -> part.items.forEach { addAll(collectImageUrls(it)) }
          is PostContentPart.Heading -> addAll(collectImageUrls(part.parts))
          is PostContentPart.Collapse -> addAll(collectImageUrls(part.parts))
          is PostContentPart.Table -> part.rows.flatten().forEach { addAll(collectImageUrls(it)) }
          else -> Unit
        }
      }
    }
}

private object ContentNormalizer {
  fun prepare(content: String): String {
    val decodedEntities =
      content
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
    return decodedEntities
      .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
      .let(com.yanga.client.api.NgaDisplayText::decodeEntities)
  }
}

/**
 * Block-level recursive descent parser for NGA post bodies.
 *
 * Document  ::= ( TextRun | Quote | Image | Emoticon )*
 * Quote     ::= "[quote]" Document "[/quote]"
 * Image     ::= ImgTag | HtmlImg | RelativeImgPath
 * Audio     ::= "[flash=audio]" MediaUrl "[/flash]"
 */
private class BbContentParser(private val source: String, private val depth: Int = 0) {
  private var pos = 0

  fun parseDocument(): List<PostContentPart> {
    if (depth >= 32) return listOf(PostContentPart.Text(source))
    val parts = mutableListOf<PostContentPart>()
    val textBuffer = StringBuilder()

    fun flushText() {
      if (textBuffer.isEmpty()) return
      val richText = InlineTextParser(textBuffer.toString()).parse()
      textBuffer.clear()
      if (richText.text.isNotEmpty()) {
        parts += PostContentPart.Text(richText.text, richText.styles)
      }
    }

    while (!atEnd()) {
      when {
        blockHeader() != null -> {
          flushText()
          parts += parseStructuredBlock(blockHeader()!!)
        }
        startsWithIgnoreCase("[hr]") -> {
          flushText()
          consume("[hr]")
          parts += PostContentPart.Rule
        }
        startsWithIgnoreCase("[quote]") -> {
          flushText()
          parts += parseQuote()
        }
        startsWithIgnoreCase("[img") -> {
          flushText()
          parseImageTag()?.let { parts += it }
        }
        startsWith("[s:") -> {
          flushText()
          parseEmoticon()?.let { parts += it }
        }
        startsWithIgnoreCase("<img") -> {
          flushText()
          parseHtmlImage()?.let { parts += it }
        }
        startsWith("./mon_") -> {
          flushText()
          parseRelativeImagePath()?.let { parts += it }
        }
        startsWithIgnoreCase("[flash") -> {
          flushText()
          parseFlashTag()?.let { parts += it }
        }
        else -> {
          textBuffer.append(source[pos])
          pos++
        }
      }
    }

    flushText()
    return parts
  }

  private fun parseQuote(): PostContentPart.Quote {
    consume("[quote]")
    val innerStart = pos
    var depth = 1
    while (!atEnd() && depth > 0) {
      when {
        startsWithIgnoreCase("[quote]") -> {
          depth++
          consume("[quote]")
        }
        startsWithIgnoreCase("[/quote]") -> {
          depth--
          if (depth == 0) break
          consume("[/quote]")
        }
        else -> pos++
      }
    }
    val inner = source.substring(innerStart, pos)
    if (depth == 0) consume("[/quote]")
    val innerParts = child(inner)
    return PostContentPart.Quote(innerParts)
  }

  private fun child(text: String): List<PostContentPart> = BbContentParser(text, depth + 1).parseDocument()

  private fun blockHeader(): MatchResult? = BLOCK_OPEN.matchAt(source, pos)

  private fun parseStructuredBlock(header: MatchResult): PostContentPart {
    val name = header.groupValues[1].lowercase()
    val argument = header.groupValues[2].removePrefix("=").trim()
    pos = header.range.last + 1
    val start = pos
    var nesting = 1
    var close: MatchResult? = null
    var inCode = false
    for (token in ALL_BLOCK_TAGS.findAll(source, start)) {
      val tokenName = token.groupValues[2].lowercase()
      val closing = token.groupValues[1] == "/"
      if (name != "code" && tokenName == "code") { inCode = !closing; continue }
      if (inCode || tokenName != name) continue
      if (closing) nesting-- else if (name != "code") nesting++
      if (nesting == 0) { close = token; break }
    }
    val body = source.substring(start, close?.range?.first ?: source.length)
    pos = close?.range?.last?.plus(1) ?: source.length
    return when (name) {
      "h" -> PostContentPart.Heading(child(body))
      "code" -> PostContentPart.Code(body.removePrefix("\n").removeSuffix("\n"), argument)
      "collapse", "spoiler" -> PostContentPart.Collapse(argument.ifBlank { "折叠内容" }, child(body))
      "list" -> {
        val items = splitTopLevel(body, "*").map { child(it.removeSuffix("[/*]")) }
        PostContentPart.ListBlock(items, argument.takeIf { it in setOf("1", "a", "A", "i", "I") })
      }
      "table" -> {
        val rowTokens = splitTopLevel(body, "tr")
        val spans = mutableListOf<List<PostContentPart.CellSpan>>()
        val rows = rowTokens.map { row ->
          val attributes = mutableListOf<String>()
          val cells = splitTopLevel(row, "td", attributes).map { child(it) }
          spans += attributes.map { tag ->
            fun span(name: String) = Regex("$name\\s*=?\\s*(\\d+)", RegexOption.IGNORE_CASE)
              .find(tag)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, 100) ?: 1
            PostContentPart.CellSpan(span("colspan"), span("rowspan"))
          }
          cells
        }
        PostContentPart.Table(rows, spans)
      }
      else -> PostContentPart.Text(body)
    }
  }

  /** Split only at the current nesting level, retaining nested rich blocks and malformed text. */
  private fun splitTopLevel(body: String, separator: String, attributes: MutableList<String>? = null): List<String> {
    val result = mutableListOf<String>()
    val buffer = StringBuilder()
    val stack = mutableListOf<String>()
    var cursor = 0
    var started = false
    var header = ""
    fun flush() {
      if (started || buffer.isNotBlank()) { result += buffer.toString(); attributes?.add(header) }
      buffer.clear()
    }
    for (tag in ALL_BLOCK_TAGS.findAll(body)) {
      buffer.append(body.substring(cursor, tag.range.first))
      val name = tag.groupValues[2].lowercase()
      val closing = tag.groupValues[1] == "/"
      if (stack.isEmpty() && name == separator) {
        flush()
        started = !closing
        header = if (closing) "" else tag.value
      } else {
        buffer.append(tag.value)
        if (name in setOf("list", "table", "quote", "collapse", "spoiler", "code", "h")) {
          if (closing && stack.lastOrNull() == name) stack.removeAt(stack.lastIndex)
          else if (!closing && stack.lastOrNull() != "code") stack += name
        }
      }
      cursor = tag.range.last + 1
    }
    buffer.append(body.substring(cursor))
    flush()
    return result
  }

  private companion object {
    val BLOCK_OPEN = Regex("\\[(list|collapse|spoiler|code|table|h)(=[^\\]]*)?\\]", RegexOption.IGNORE_CASE)
    val ALL_BLOCK_TAGS = Regex("\\[(/?)(list|table|quote|collapse|spoiler|code|h|tr|td|\\*)(?:\\d+|[=\\s][^\\]]*)?\\]", RegexOption.IGNORE_CASE)
  }

  private fun parseImageTag(): PostContentPart.Image? {
    if (!startsWithIgnoreCase("[img")) return null
    val headerEnd = source.indexOf(']', pos)
    if (headerEnd == -1) {
      pos = source.length
      return null
    }
    val header = source.substring(pos, headerEnd + 1)
    pos = headerEnd + 1

    extractSrcAttribute(header)?.let { src ->
      return PostContentPart.Image(normalizeContentImageUrl(src))
    }

    val closeTag = indexOfIgnoreCase("[/img]", pos)
    if (closeTag == -1) {
      return null
    }
    val url = source.substring(pos, closeTag).trim()
    pos = closeTag + "[/img]".length
    if (url.isBlank()) return null
    return PostContentPart.Image(normalizeContentImageUrl(url))
  }

  private fun parseEmoticon(): PostContentPart.Emoticon? {
    if (!startsWith("[s:")) return null
    val close = source.indexOf(']', pos)
    if (close == -1) {
      pos = source.length
      return null
    }
    val token = source.substring(pos, close + 1)
    pos = close + 1
    val body = token.substring(3, token.length - 1)
    val separator = body.indexOf(':')
    if (separator == -1) return null
    val category = body.substring(0, separator)
    val alt = body.substring(separator + 1)
    return resolveEmoticon(category, alt)
  }

  private fun parseHtmlImage(): PostContentPart.Image? {
    if (!startsWithIgnoreCase("<img")) return null
    val close = source.indexOf('>', pos)
    if (close == -1) {
      pos = source.length
      return null
    }
    val tag = source.substring(pos, close + 1)
    pos = close + 1
    val src = extractSrcAttribute(tag) ?: return null
    return PostContentPart.Image(normalizeContentImageUrl(src))
  }

  private fun parseFlashTag(): PostContentPart? {
    if (!startsWithIgnoreCase("[flash")) return null
    val headerEnd = source.indexOf(']', pos)
    if (headerEnd == -1) {
      pos = source.length
      return null
    }
    val header = source.substring(pos + 1, headerEnd)
    pos = headerEnd + 1
    val flashType =
      header
        .substringAfter("flash=", header)
        .substringBefore(',')
        .trim()
        .lowercase()
    val closeTag = indexOfIgnoreCase("[/flash]", pos)
    if (closeTag == -1) {
      return null
    }
    val rawUrl = source.substring(pos, closeTag).trim()
    pos = closeTag + "[/flash]".length
    if (rawUrl.isBlank()) return null

    return when (flashType) {
      "audio" -> {
        val url = normalizeContentMediaUrl(rawUrl)
        PostContentPart.Audio(
          url = url,
          label = ImageUrlResolver.fileName(url),
        )
      }
      else -> null
    }
  }

  private fun parseRelativeImagePath(): PostContentPart.Image? {
    if (!startsWith("./mon_")) return null
    val start = pos
    while (pos < source.length) {
      val ch = source[pos]
      if (ch.isWhitespace() || ch == '"' || ch == '\'' || ch == '<' || ch == '>' || ch == '[' || ch == ']') break
      pos++
    }
    val path = source.substring(start, pos)
    if (path.isBlank()) return null
    return PostContentPart.Image(normalizeContentImageUrl(path))
  }

  private fun atEnd(): Boolean = pos >= source.length

  private fun startsWith(value: String): Boolean = source.startsWith(value, pos)

  private fun startsWithIgnoreCase(value: String): Boolean =
    source.regionMatches(pos, value, 0, value.length, ignoreCase = true)

  private fun consume(value: String) {
    pos += value.length
  }

  private fun indexOfIgnoreCase(value: String, from: Int = pos): Int {
    val haystack = source.lowercase()
    val needle = value.lowercase()
    return haystack.indexOf(needle, from)
  }
}

/**
 * Inline parser that walks text and BBCode style tags, producing styled plain text.
 */
private class InlineTextParser(private val source: String) {
  private var pos = 0
  private val output = StringBuilder()
  private val styles = mutableListOf<PostTextStyleRange>()
  private val stack = mutableListOf<OpenStyle>()

  fun parse(): RichText {
    while (!atEnd()) {
      when {
        source[pos] == '[' -> {
          val tag = readBracketTag()
          if (tag != null) {
            applyTag(tag)
          } else {
            output.append('[')
            pos++
          }
        }
        source[pos] == '<' -> {
          if (!consumeHtmlMarkup()) {
            output.append('<')
            pos++
          }
        }
        source[pos] == '{' -> {
          if (!skipLayoutTag()) {
            output.append(source[pos])
            pos++
          }
        }
        else -> {
          output.append(source[pos])
          pos++
        }
      }
    }
    closeAllOpenTags()
    addStandaloneUrlStyles()
    return finalizeRichText()
  }

  private fun readBracketTag(): Tag? {
    if (source[pos] != '[') return null
    val close = source.indexOf(']', pos)
    if (close == -1) return null
    val raw = source.substring(pos + 1, close)
    if (raw.isEmpty()) return null
    pos = close + 1

    val closing = raw.startsWith("/")
    val body = if (closing) raw.substring(1) else raw
    val equals = body.indexOf('=')
    val name: String
    val arg: String?
    if (equals == -1) {
      name = body
      arg = null
    } else {
      name = body.substring(0, equals)
      arg = body.substring(equals + 1)
    }
    return Tag(name.lowercase(), arg?.trim()?.ifBlank { null }, closing, source.substring(pos - raw.length - 2, pos))
  }

  private fun applyTag(tag: Tag) {
    if (tag.name == "align") return
    if (tag.name == "*") {
      if (!tag.closing) output.append("\n• ")
      return
    }
    if (!isInlineTag(tag.name)) {
      output.append(tag.raw)
      return
    }
    if (tag.closing) {
      closeStyle(tag.name)
    } else {
      openStyle(tag.name, tag.arg.orEmpty())
    }
  }

  private fun isInlineTag(name: String): Boolean =
    name in INLINE_TAGS

  private fun openStyle(tag: String, arg: String) {
    val style =
      when (tag) {
        "b" -> ActiveStyle(bold = true)
        "i" -> ActiveStyle(italic = true)
        "u" -> ActiveStyle(underline = true)
        "del" -> ActiveStyle(strikeThrough = true)
        "color" -> ActiveStyle(color = arg.ifBlank { null })
        "size" -> ActiveStyle(sizePercent = arg.removeSuffix("%").toIntOrNull())
        "url" -> ActiveStyle(linkUrl = arg.ifBlank { null })
        "uid" -> ActiveStyle(linkUrl = arg.ifBlank { null }?.let { "nga://user/$it" })
        "tid" -> ActiveStyle(linkUrl = arg.ifBlank { null }?.let { "nga://thread/$it" })
        "pid" -> ActiveStyle(linkUrl = pidLinkUrl(arg))
        else -> ActiveStyle()
      }
    stack += OpenStyle(tag, output.length, style)
  }

  private fun closeStyle(tag: String) {
    val index = stack.indexOfLast { it.tag == tag }
    if (index == -1) return
    val open = stack.removeAt(index)
    val end = output.length
    if (open.start >= end) return
    val linkUrl =
      when {
        open.style.linkUrl != null -> open.style.linkUrl
        open.tag == "url" -> output.substring(open.start, end).trim().ifBlank { null }
        else -> null
      }
    styles +=
      PostTextStyleRange(
        start = open.start,
        end = end,
        bold = open.style.bold,
        italic = open.style.italic,
        underline = open.style.underline,
        strikeThrough = open.style.strikeThrough,
        color = open.style.color,
        sizePercent = open.style.sizePercent,
        linkUrl = linkUrl,
      )
  }

  private fun closeAllOpenTags() {
    while (stack.isNotEmpty()) {
      closeStyle(stack.last().tag)
    }
  }

  private fun consumeHtmlMarkup(): Boolean {
    if (!source.startsWith("<", pos)) return false
    val close = source.indexOf('>', pos)
    if (close == -1) return false
    val tag = source.substring(pos, close + 1)
    pos = close + 1
    if (tag.startsWith("<br", ignoreCase = true)) {
      output.append('\n')
    }
    return true
  }

  private fun skipLayoutTag(): Boolean {
    val start = pos
    if (source[start] != '{' && source[start] != '[') return false
    val close = source.indexOfAny(charArrayOf(']', '}'), start)
    if (close == -1) return false
    val token = source.substring(start, close + 1)
    if (!token.contains("align", ignoreCase = true)) return false
    pos = close + 1
    return true
  }

  private fun addStandaloneUrlStyles() {
    val text = output.toString()
    var searchFrom = 0
    while (searchFrom < text.length) {
      val http = text.indexOf("http://", searchFrom, ignoreCase = true)
      val https = text.indexOf("https://", searchFrom, ignoreCase = true)
      val start =
        when {
          http == -1 -> https
          https == -1 -> http
          else -> minOf(http, https)
        }
      if (start == -1) break
      var end = start
      while (end < text.length && !text[end].isWhitespace() && text[end] !in "\"'<>[]") {
        end++
      }
      while (end > start && text[end - 1] in ".,;:!?)]}") {
        end--
      }
      if (styles.none { rangesOverlap(start, end, it.start, it.end) }) {
        styles += PostTextStyleRange(start = start, end = end, linkUrl = text.substring(start, end))
      }
      searchFrom = end
    }
  }

  private fun finalizeRichText(): RichText {
    val normalized = normalizeWhitespace(output.toString())
    val adjustedStyles =
      if (normalized.leadingTrim == 0 && normalized.trailingTrim == 0) {
        styles.filter { it.start < it.end }
      } else {
        styles.mapNotNull { style ->
          val start = (style.start - normalized.leadingTrim).coerceAtLeast(0)
          val end = (style.end - normalized.leadingTrim).coerceAtMost(normalized.text.length)
          if (start < end) style.copy(start = start, end = end) else null
        }
      }
    return RichText(normalized.text, adjustedStyles).collapseReplyToPostLinkPrefix()
  }

  private fun atEnd(): Boolean = pos >= source.length

  private data class Tag(
    val name: String,
    val arg: String?,
    val closing: Boolean,
    val raw: String,
  )

  private companion object {
    val INLINE_TAGS = setOf("b", "i", "u", "del", "color", "size", "url", "uid", "tid", "pid")
  }
}

private fun pidLinkUrl(arg: String): String? {
  val parts = arg.split(',')
  val postId = parts.getOrNull(0)?.trim()?.takeIf { it.all(Char::isDigit) } ?: return null
  val threadId = parts.getOrNull(1)?.trim()?.takeIf { it.all(Char::isDigit) }
  val page = parts.getOrNull(2)?.trim()?.toIntOrNull()?.coerceAtLeast(1)
  val query =
    buildList {
      threadId?.let { add("tid=$it") }
      page?.let { add("page=$it") }
    }.joinToString("&")
  return if (query.isBlank()) {
    "nga://post/$postId"
  } else {
    "nga://post/$postId?$query"
  }
}

private fun RichText.collapseReplyToPostLinkPrefix(): RichText {
  val prefix = "Reply to "
  val linkedReply = "Reply"
  if (!text.startsWith(prefix + linkedReply + " ")) return this
  val linkStart = prefix.length
  val linkEnd = linkStart + linkedReply.length
  val hasPostLink =
    styles.any { style ->
      style.start == linkStart &&
        style.end == linkEnd &&
        style.linkUrl?.startsWith("nga://post/", ignoreCase = true) == true
    }
  if (!hasPostLink) return this

  val collapsedText = text.removeRange(0, prefix.length)
  val collapsedStyles =
    styles.mapNotNull { style ->
      style.shiftAfterRemoving(start = 0, end = prefix.length, textLength = collapsedText.length)
    }
  return RichText(collapsedText, collapsedStyles)
}

private fun PostTextStyleRange.shiftAfterRemoving(
  start: Int,
  end: Int,
  textLength: Int,
): PostTextStyleRange? {
  val removedLength = end - start
  val shiftedStart =
    when {
      this.start >= end -> this.start - removedLength
      this.start >= start -> start
      else -> this.start
    }.coerceIn(0, textLength)
  val shiftedEnd =
    when {
      this.end >= end -> this.end - removedLength
      this.end > start -> start
      else -> this.end
    }.coerceIn(0, textLength)
  return if (shiftedStart < shiftedEnd) copy(start = shiftedStart, end = shiftedEnd) else null
}

private fun extractSrcAttribute(tag: String): String? {
  val lower = tag.lowercase()
  val marker = "src"
  val index = lower.indexOf(marker)
  if (index == -1) return null
  var cursor = index + marker.length
  while (cursor < tag.length && tag[cursor].isWhitespace()) cursor++
  if (cursor >= tag.length || tag[cursor] != '=') return null
  cursor++
  while (cursor < tag.length && tag[cursor].isWhitespace()) cursor++
  if (cursor >= tag.length) return null
  return when (tag[cursor]) {
    '"', '\'' -> {
      val quote = tag[cursor]
      cursor++
      val end = tag.indexOf(quote, cursor)
      if (end == -1) null else tag.substring(cursor, end).trim()
    }
    else -> {
      val end = tag.indexOfAny(charArrayOf(' ', '>', ']'), cursor)
      if (end == -1) tag.substring(cursor).trim() else tag.substring(cursor, end).trim()
    }
  }
}

private fun resolveEmoticon(category: String, alt: String): PostContentPart.Emoticon? {
  val image = NgaEmoticons.resolve(category, alt) ?: return null
  val url = NgaStaticUrls.emoticonBaseUrl + image
  return PostContentPart.Emoticon(code = "[s:$category:$alt]", url = url, alt = alt)
}

private fun normalizeContentMediaUrl(url: String): String =
  normalizeContentImageUrl(sanitizeMediaUrl(url))

private fun sanitizeMediaUrl(url: String): String =
  url.trimEnd('″', '"', '\u2033', '\u201d')

private fun normalizeContentImageUrl(url: String): String {
  val normalized =
    url
      .replace(Regex("(http\\S+)\\.gif\\.(thumb_s|medium|thumb|thumb_ss)\\.jpg", RegexOption.IGNORE_CASE), "$1.gif")
      .replace(Regex("(http\\S+)\\.(png|jpg)\\.(thumb_s|medium|thumb|thumb_ss)\\.jpg", RegexOption.IGNORE_CASE), "$1.$2")
  return ImageUrlResolver.resolve(normalized)
}

private fun rangesOverlap(start: Int, end: Int, otherStart: Int, otherEnd: Int): Boolean =
  start < otherEnd && otherStart < end

private fun normalizeWhitespace(value: String): NormalizedText {
  val normalized = value.replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
  val trimmed = normalized.trim()
  return NormalizedText(
    text = trimmed,
    leadingTrim = normalized.length - normalized.trimStart().length,
    trailingTrim = normalized.length - normalized.trimEnd().length,
  )
}

private data class RichText(
  val text: String,
  val styles: List<PostTextStyleRange>,
)

private data class NormalizedText(
  val text: String,
  val leadingTrim: Int,
  val trailingTrim: Int,
)

private data class OpenStyle(
  val tag: String,
  val start: Int,
  val style: ActiveStyle,
)

private data class ActiveStyle(
  val bold: Boolean = false,
  val italic: Boolean = false,
  val underline: Boolean = false,
  val strikeThrough: Boolean = false,
  val color: String? = null,
  val sizePercent: Int? = null,
  val linkUrl: String? = null,
)

private object NgaEmoticons {
  private val ac =
    mapOf(
      "blink" to "ac0.png",
      "goodjob" to "ac1.png",
      "上" to "ac2.png",
      "中枪" to "ac3.png",
      "偷笑" to "ac4.png",
      "冷" to "ac5.png",
      "凌乱" to "ac6.png",
      "反对" to "ac7.png",
      "吓" to "ac8.png",
      "吻" to "ac9.png",
      "呆" to "ac10.png",
      "咦" to "ac11.png",
      "哦" to "ac12.png",
      "哭" to "ac13.png",
      "哭1" to "ac14.png",
      "哭笑" to "ac15.png",
      "哼" to "ac16.png",
      "喘" to "ac17.png",
      "喷" to "ac18.png",
      "嘲笑" to "ac19.png",
      "嘲笑1" to "ac20.png",
      "囧" to "ac21.png",
      "委屈" to "ac22.png",
      "心" to "ac23.png",
      "忧伤" to "ac24.png",
      "怒" to "ac25.png",
      "怕" to "ac26.png",
      "惊" to "ac27.png",
      "愁" to "ac28.png",
      "抓狂" to "ac29.png",
      "抠鼻" to "ac30.png",
      "擦汗" to "ac31.png",
      "无语" to "ac32.png",
      "晕" to "ac33.png",
      "汗" to "ac34.png",
      "瞎" to "ac35.png",
      "羞" to "ac36.png",
      "羡慕" to "ac37.png",
      "花痴" to "ac38.png",
      "茶" to "ac39.png",
      "衰" to "ac40.png",
      "计划通" to "ac41.png",
      "赞同" to "ac43.png",
      "闪光" to "ac42.png",
      "黑枪" to "ac44.png",
    )

  private val a2 =
    linkedMapOf(
      "goodjob" to "a2_02.png",
      "诶嘿" to "a2_05.png",
      "偷笑" to "a2_03.png",
      "怒" to "a2_04.png",
      "笑" to "a2_07.png",
      "那个…" to "a2_08.png",
      "哦嗬嗬嗬" to "a2_09.png",
      "舔" to "a2_10.png",
      "鬼脸" to "a2_14.png",
      "冷" to "a2_16.png",
      "大哭" to "a2_15.png",
      "哭" to "a2_17.png",
      "恨" to "a2_21.png",
      "中枪" to "a2_23.png",
      "囧" to "a2_24.png",
      "你看看你" to "a2_25.png",
      "doge" to "a2_27.png",
      "自戳双目" to "a2_28.png",
      "偷吃" to "a2_30.png",
      "冷笑" to "a2_31.png",
      "壁咚" to "a2_32.png",
      "不活了" to "a2_33.png",
      "不明觉厉" to "a2_36.png",
      "是在下输了" to "a2_51.png",
      "你为猴这么" to "a2_53.png",
      "干杯" to "a2_54.png",
      "干杯2" to "a2_55.png",
      "异议" to "a2_47.png",
      "认真" to "a2_48.png",
      "你已经死了" to "a2_45.png",
      "你这种人…" to "a2_49.png",
      "妮可妮可妮" to "a2_18.png",
      "惊" to "a2_19.png",
      "抢镜头" to "a2_52.png",
      "yes" to "a2_26.png",
      "有何贵干" to "a2_11.png",
      "病娇" to "a2_12.png",
      "lucky" to "a2_13.png",
      "poi" to "a2_20.png",
      "囧2" to "a2_22.png",
      "威吓" to "a2_42.png",
      "jojo立" to "a2_37.png",
      "jojo立2" to "a2_38.png",
      "jojo立3" to "a2_39.png",
      "jojo立4" to "a2_41.png",
      "jojo立5" to "a2_40.png",
    )

  private val ng =
    linkedMapOf(
      "呲牙笑" to "ng_1.png",
      "奸笑" to "ng_2.png",
      "问号" to "ng_3.png",
      "茶" to "ng_4.png",
      "笑指" to "ng_5.png",
      "燃尽" to "ng_6.png",
      "晕" to "ng_7.png",
      "扇笑" to "ng_8.png",
      "寄" to "ng_9.png",
      "别急" to "ng_10.png",
      "doge" to "ng_11.png",
      "丧" to "ng_12.png",
      "汗" to "ng_13.png",
      "叹气" to "ng_15.png",
      "吃饼" to "ng_16.png",
      "吃瓜" to "ng_17.png",
      "吐舌" to "ng_18.png",
      "哭" to "ng_19.png",
      "喘" to "ng_20.png",
      "心" to "ng_21.png",
      "喷" to "ng_22.png",
      "困" to "ng_24.png",
      "大哭" to "ng_25.png",
      "大惊" to "ng_26.png",
      "害怕" to "ng_27.png",
      "惊" to "ng_28.png",
      "暴怒" to "ng_30.png",
      "气愤" to "ng_31.png",
      "热" to "ng_32.png",
      "瓜不熟" to "ng_33.png",
      "瞎" to "ng_34.png",
      "色" to "ng_35.png",
      "斜眼" to "ng_37.png",
      "问号大" to "ng_38.png",
    )

  private val pg =
    linkedMapOf(
      "战斗力" to "pg01.png",
      "哈啤" to "pg02.png",
      "满分" to "pg03.png",
      "衰" to "pg04.png",
      "拒绝" to "pg05.png",
      "心" to "pg06.png",
      "严肃" to "pg07.png",
      "吃瓜" to "pg08.png",
      "嘣" to "pg09.png",
      "嘣2" to "pg10.png",
      "冻" to "pg11.png",
      "谢" to "pg12.png",
      "哭" to "pg13.png",
      "响指" to "pg14.png",
      "转身" to "pg15.png",
    )

  private val pst =
    listOf(
      "举手",
      "亲",
      "偷笑",
      "偷笑2",
      "偷笑3",
      "傻眼",
      "傻眼2",
      "兔子",
      "发光",
      "呆",
      "呆2",
      "呆3",
      "呕",
      "呵欠",
      "哭",
      "哭2",
      "哭3",
      "嘲笑",
      "基",
      "宅",
      "安慰",
      "幸福",
      "开心",
      "开心2",
      "开心3",
      "怀疑",
      "怒",
      "怒2",
      "怨",
      "惊吓",
      "惊吓2",
      "惊呆",
      "惊呆2",
      "惊呆3",
      "惨",
      "斜眼",
      "晕",
      "汗",
      "泪",
      "泪2",
      "泪3",
      "泪4",
      "满足",
      "满足2",
      "火星",
      "牙疼",
      "电击",
      "看戏",
      "眼袋",
      "眼镜",
      "笑而不语",
      "紧张",
      "美味",
      "背",
      "脸红",
      "脸红2",
      "腐",
      "星星眼",
      "谢",
      "醉",
      "闷",
      "闷2",
      "音乐",
      "黑脸",
      "鼻血",
    ).mapIndexed { index, code -> code to "pt${index.toString().padStart(2, '0')}.png" }.toMap()

  private val dt =
    listOf(
      "ROLL",
      "上",
      "傲娇",
      "叉出去",
      "发光",
      "呵欠",
      "哭",
      "啃古头",
      "嘲笑",
      "心",
      "怒",
      "怒2",
      "怨",
      "惊",
      "惊2",
      "无语",
      "星星眼",
      "星星眼2",
      "晕",
      "注意",
      "注意2",
      "泪",
      "泪2",
      "烧",
      "笑",
      "笑2",
      "笑3",
      "脸红",
      "药",
      "衰",
      "鄙视",
      "闲",
      "黑脸",
    ).mapIndexed { index, code -> code to "dt${(index + 1).toString().padStart(2, '0')}.png" }.toMap()

  private val groups = mapOf("ac" to ac, "a2" to a2, "ng" to ng, "pg" to pg, "pst" to pst, "dt" to dt)

  fun resolve(category: String, code: String): String? = groups[category.lowercase()]?.get(code)
}
