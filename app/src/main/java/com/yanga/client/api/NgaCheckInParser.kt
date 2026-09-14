package com.yanga.client.api

object NgaCheckInParser {
  fun parse(raw: String): String {
    val root = ngaJsonRoot(raw)
    val data = root.objectValue("data") ?: root
    val message = data.stringValue("0", "message")
    if (message.contains("签到成功") || message.contains("今天已经签到")) return message
    val error = root.opt("error")?.toString().orEmpty()
    throw NgaApiException(error.ifBlank { message.ifBlank { "无法确认签到结果，请重试" } })
  }
}
