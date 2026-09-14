package com.yanga.client.api

object NgaReactionParser {
  fun requireSuccess(raw: String) {
    val root = ngaJsonRoot(raw)
    val error = root.opt("error")
    if (error != null && error != org.json.JSONObject.NULL) {
      throw NgaApiException((error as? org.json.JSONObject)?.optString("0")?.takeIf { it.isNotBlank() } ?: error.toString())
    }
    val message = root.optJSONObject("data")?.optString("0").orEmpty()
    if (message.toIntOrNull() == null && !message.startsWith("操作成功") && message != "成功") {
      throw NgaApiException(message.ifBlank { "未能确认赞踩结果，请刷新后检查" })
    }
  }
}
