package com.yanga.client.api

import java.net.URI
import kotlin.io.encoding.Base64
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Matches the forum's js_imageEdit.js setAvatar2 flow. */
object NgaAvatarUpload {
  data class Ticket(val url: String, val checksum: String)

  private fun data(raw: String): Any {
    val root = ngaJsonRoot(raw)
    if (root.has("error") && root.opt("error") !in listOf(false, 0, "", JSONObject.NULL)) {
      throw NgaApiException(root.stringValue("errorinfo", "error"))
    }
    return root.opt("data") ?: throw NgaApiException("头像服务未返回有效数据，请重试")
  }

  private fun item(value: Any, index: Int): Any? = when (value) {
    is JSONObject -> value.opt(index.toString())
    is JSONArray -> value.opt(index)
    else -> null
  }

  fun ticket(raw: String): Ticket {
    val data = data(raw)
    val checksum = item(data, 1) as? String ?: throw NgaApiException("未取得头像上传凭证")
    val address = item(data, 2) as? String ?: throw NgaApiException("未取得头像上传地址")
    val secureAddress = when {
      address.startsWith("//") -> "https:$address"
      address.startsWith("http://") -> "https://" + address.removePrefix("http://")
      else -> address
    }
    val uri = runCatching { URI(secureAddress) }.getOrNull()
    val host = uri?.host.orEmpty().lowercase()
    require(uri?.scheme == "https" && uri.userInfo == null &&
      listOf("nga.cn", "nga.178.com", "ngacn.cc", "ngabbs.com").any { host == it || host.endsWith(".$it") }) {
      "论坛返回了不支持的头像上传地址"
    }
    val query = uri.rawQuery.orEmpty().split('&').filter { it.isNotEmpty() && !it.startsWith("checksum=") }.joinToString("&")
    val url = secureAddress.substringBefore('?') + if (query.isEmpty()) "" else "?$query"
    return Ticket(url, checksum)
  }

  fun request(ticket: Ticket, uid: String, png: ByteArray): NgaRequest {
    require(uid.toLongOrNull()?.let { it > 0 } == true)
    require(png.isNotEmpty() && png.size <= 1024 * 1024) { "头像图片大小无效" }
    val fields = linkedMapOf(
      "n${uid}_0" to "data:image/png;base64,${Base64.encode(png)}",
      "func" to "upload", "avatar" to "1", "uid" to uid, "lite" to "js", "checksum" to ticket.checksum,
    )
    val boundary = "Yanga${UUID.randomUUID()}"
    val body = buildString {
      fields.forEach { (name, value) ->
        append("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n")
      }
      append("--$boundary--\r\n")
    }.toByteArray(Charsets.UTF_8)
    return NgaRequest(NgaHttpMethod.POST, ticket.url,
      headers = mapOf("Content-Type" to "multipart/form-data; boundary=$boundary"), binaryBody = body)
  }

  fun parse(raw: String): String {
    val data = data(raw)
    val payload = (item(data, 0) as? JSONObject) ?: (data as? JSONObject)
    return payload?.nullableStringValue("url")
      ?: throw NgaApiException("头像上传未返回图片信息，请重试")
  }

  fun requireSaved(raw: String) {
    val value = data(raw)
    val message = item(value, 0)?.toString().orEmpty()
    if (!message.contains("成功")) throw NgaApiException(message.ifBlank { "未能确认头像保存结果，请重新加载资料后检查" })
  }
}
