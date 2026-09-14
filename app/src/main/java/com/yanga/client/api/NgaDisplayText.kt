package com.yanga.client.api

/** Decode one layer only: escaped markup in a title is still literal text. */
object NgaDisplayText {
  private val entity = Regex("&(#(?:[xX][0-9a-fA-F]+|[0-9]+)|[A-Za-z]+);")
  private val lineBreak = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
  private val whitespace = Regex("[\\s\\u00a0]+")
  private val named = mapOf(
    "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
    "nbsp" to " ", "ensp" to " ", "emsp" to " ", "thinsp" to " ",
    "ndash" to "–", "mdash" to "—", "hellip" to "…", "middot" to "·",
    "lsquo" to "‘", "rsquo" to "’", "ldquo" to "“", "rdquo" to "”",
    "copy" to "©", "reg" to "®", "trade" to "™", "times" to "×", "divide" to "÷",
    "bull" to "•", "yen" to "¥", "euro" to "€", "pound" to "£",
  )

  fun decodeEntities(value: String): String = entity.replace(value) { match ->
    val token = match.groupValues[1]
    if (!token.startsWith('#')) {
      named[token] ?: match.value
    } else {
      val number = token.drop(1)
      val codePoint = if (number.startsWith('x', ignoreCase = true)) {
        number.drop(1).toIntOrNull(16)
      } else {
        number.toIntOrNull()
      }
      if (codePoint != null && codePoint in 1..0x10ffff && codePoint !in 0xd800..0xdfff) {
        String(Character.toChars(codePoint))
      } else {
        match.value
      }
    }
  }

  fun singleLine(value: String): String =
    whitespace.replace(decodeEntities(lineBreak.replace(value, " ")), " ").trim()
}
