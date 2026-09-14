package com.yanga.client.api

data class NgaReactionResult(val reaction: Int? = null, val score: Int? = null)

object NgaReactionParser {
  fun requireSuccess(raw: String) { parse(raw) }

  fun parse(raw: String): NgaReactionResult {
    val root = ngaJsonRoot(raw)
    val error = root.opt("error")
    if (error != null && error != org.json.JSONObject.NULL) {
      throw NgaApiException((error as? org.json.JSONObject)?.optString("0")?.takeIf { it.isNotBlank() } ?: error.toString())
    }
    val message = root.optJSONObject("data")?.optString("0").orEmpty()
    val reaction = when (message.trim()) {
      "你对这个帖子表示支持" -> 1
      "你对这个帖子表示反对" -> -1
      "你取消了对这个帖子的支持", "你取消了对这个帖子的反对" -> 0
      else -> null
    }
    if (reaction != null) return NgaReactionResult(reaction)
    if (message.toIntOrNull() == null && !message.startsWith("操作成功") && message != "成功") {
      throw NgaApiException(message.ifBlank { "未能确认赞踩结果，请刷新后检查" })
    }
    return NgaReactionResult()
  }
}
