package com.yanga.client.api

object NgaReplyParser {
  fun requireSuccess(raw: String) {
    val root = ngaJsonRoot(raw)
    val error = root.opt("error")
    if (error != null && error != org.json.JSONObject.NULL) {
      throw NgaApiException((error as? org.json.JSONObject)?.optString("0")?.takeIf { it.isNotBlank() } ?: error.toString())
    }
    val message = root.optJSONObject("data")?.optString("0").orEmpty()
    if (!message.startsWith("发贴完毕") && !message.startsWith("发帖完毕") &&
      !message.startsWith("发表成功") && !message.startsWith("回复成功") && !message.startsWith("操作成功")) {
      throw NgaApiException(message.ifBlank { "未能确认发送结果，请查看主题后再决定是否重试" })
    }
  }
}
