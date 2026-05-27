package com.yanga.client.api

object NgaResponseNormalizer {
  private const val WRAPPER_PREFIX = "window.script_muti_get_var_store="
  private const val ERROR_FILL = "/*error fill content"
  private const val JS_MARKER = "/*\$js\$*/"

  fun normalize(raw: String): String {
    var value = raw.trim()
    if (value.startsWith(WRAPPER_PREFIX)) {
      value = value.removePrefix(WRAPPER_PREFIX)
    }
    val errorFillIndex = value.indexOf(ERROR_FILL)
    if (errorFillIndex >= 0) {
      value = value.substring(0, errorFillIndex)
    }
    return value
      .replace(JS_MARKER, "")
      .replace(Regex("\"content\":\\+(\\d+),"), "\"content\":\"+\$1\",")
      .replace(Regex("\"subject\":\\+(\\d+),"), "\"subject\":\"+\$1\",")
      .trim()
  }
}
