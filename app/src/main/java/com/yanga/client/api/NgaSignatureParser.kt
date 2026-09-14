package com.yanga.client.api

object NgaSignatureParser {
  fun requireSuccess(raw: String) {
    val root = ngaJsonRoot(raw)
    root.opt("error")?.let { throw NgaApiException(it.toString()) }
    val message = (root.objectValue("data") ?: root).stringValue("0", "message")
    if (!message.contains("操作成功")) throw NgaApiException(message.ifBlank { "未能确认签名保存结果，请重新加载资料后检查" })
  }
}
