package com.yanga.client.api

object NgaResponseNormalizer {
  private const val WRAPPER_PREFIX = "window.script_muti_get_var_store="
  private const val ERROR_FILL = "/*error fill content"
  private const val JS_MARKER = "/*\$js\$*/"

  fun normalize(raw: String): String {
    val value = raw.trim()
    
    val firstBrace = value.indexOf('{')
    val firstBracket = value.indexOf('[')
    val start = when {
      firstBrace >= 0 && firstBracket >= 0 -> minOf(firstBrace, firstBracket)
      firstBrace >= 0 -> firstBrace
      firstBracket >= 0 -> firstBracket
      else -> return value
    }
    
    val lastBrace = value.lastIndexOf('}')
    val lastBracket = value.lastIndexOf(']')
    val end = maxOf(lastBrace, lastBracket)
    
    if (end <= start) return value
    
    val json = value.substring(start, end + 1)

    return json
      .replace(JS_MARKER, "")
      .replace(Regex("\"content\":\\+(\\d+),"), "\"content\":\"+\$1\",")
      .replace(Regex("\"subject\":\\+(\\d+),"), "\"subject\":\"+\$1\",")
      .trim()
  }
}
