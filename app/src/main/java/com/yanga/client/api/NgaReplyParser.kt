package com.yanga.client.api

object NgaReplyParser {
  fun requireSuccess(raw: String) {
    val root = ngaJsonRoot(raw)
    val error = root.opt("error")
    if (error != null && error != org.json.JSONObject.NULL) {
      throw NgaApiException((error as? org.json.JSONObject)?.optString("0")?.takeIf { it.isNotBlank() } ?: error.toString())
    }
    val data = root.opt("data") ?: root
    val status = (data as? org.json.JSONObject)?.opt("__MESSAGE")
    fun value(container: Any?, index: Int): String = when (container) {
      is org.json.JSONObject -> container.optString(index.toString())
      is org.json.JSONArray -> container.optString(index)
      else -> ""
    }
    val message = value(status ?: data, if (status != null) 1 else 0).trim()
    if (status != null && value(status, 3) != "200") {
      throw NgaApiException(message.ifBlank { "发送失败，请查看主题后再决定是否重试" })
    }
    if (status != null && (message.contains("发贴完毕") || message.contains("发帖完毕"))) return
    if (!message.startsWith("发贴完毕") && !message.startsWith("发帖完毕") &&
      !message.startsWith("发表成功") && !message.startsWith("回复成功") && !message.startsWith("操作成功")) {
      throw NgaApiException(message.ifBlank { "未能确认发送结果，请查看主题后再决定是否重试" })
    }
  }
}
