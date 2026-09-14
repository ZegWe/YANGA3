package com.yanga.client.api

import org.json.JSONArray
import org.json.JSONObject

object NgaCheckInParser {
  fun parse(raw: String): String {
    val root = ngaJsonRoot(raw)
    val error = message(root.opt("error"))
    // NGA returns duplicate check-ins in the error envelope, even though they
    // confirm the account is already checked in for the current server day.
    if (alreadyCheckedIn(error)) return error
    if (error.isNotBlank()) throw NgaApiException(error)
    val text = message(root.opt("data") ?: root)
    if (text.startsWith("签到成功") || alreadyCheckedIn(text)) return text
    throw NgaApiException(text.ifBlank { "无法确认签到结果，请重试" })
  }

  private fun alreadyCheckedIn(text: String): Boolean =
    text.startsWith("你今天已经签到") || text.startsWith("今天已经签到")

  private fun message(value: Any?): String = when (value) {
    is JSONObject -> message(value.opt("0") ?: value.opt("message"))
    is JSONArray -> message(value.opt(0))
    is String -> value.trim()
    else -> ""
  }
}
