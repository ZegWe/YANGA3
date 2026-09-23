package com.yanga.client.api

import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.UUID
import org.json.JSONObject

data class TopicAttachment(val name: String, val id: String, val check: String, val url: String, val image: Boolean) : java.io.Serializable {
  val markup: String get() = if (image) "[img]$url[/img]" else "[url=$url]${name.replace("[", "").replace("]", "")}[/url]"
}

object NgaTopicPosting {
  const val MAX_FILE_BYTES = 20 * 1024 * 1024

  private fun data(raw: String): JSONObject {
    val root = ngaJsonRoot(raw)
    val error = root.opt("error")
    if (error != null && error !in listOf(JSONObject.NULL, false, 0, "")) {
      throw NgaApiException((error as? JSONObject)?.optString("0")?.takeIf { it.isNotBlank() } ?: error.toString())
    }
    if (root.optInt("error_code", 0) != 0) {
      throw NgaApiException(root.stringValue("error_message", "message").ifBlank {
        if (root.optInt("error_code") == 9) "附件过大，请压缩后重试" else "附件上传失败（错误码 ${root.optInt("error_code")}）"
      })
    }
    val data = root.optJSONObject("data") ?: throw NgaApiException("论坛未返回有效数据，请尝试网页发帖")
    if (data.optInt("error_code", 0) != 0) {
      throw NgaApiException(data.stringValue("error", "message", "error_code"))
    }
    return data
  }

  fun auth(raw: String): String = data(raw).optString("auth").takeIf { it.isNotBlank() }
    ?: throw NgaApiException("无法取得附件上传权限，请尝试网页发帖")

  fun uploaded(raw: String, name: String, image: Boolean): TopicAttachment {
    val data = data(raw)
    val id = data.optString("attachments").trim()
    val check = data.optString("attachments_check").trim()
    val rawUrl = data.optString("url").trim()
    val url = when {
      // attach.php returns a bare mon_YYYYMM/... path; the reference client
      // prepends "./" when inserting it into an [img] tag.
      rawUrl.startsWith("mon_") -> "https://img.nga.cn/attachments/$rawUrl"
      rawUrl.startsWith("/mon_") -> "https://img.nga.cn/attachments$rawUrl"
      rawUrl.startsWith("./") -> "https://img.nga.cn/attachments/" + rawUrl.removePrefix("./")
      rawUrl.startsWith("//") -> "https:$rawUrl"
      rawUrl.startsWith("http://") -> "https:" + rawUrl.removePrefix("http:")
      else -> rawUrl
    }
    val uri = runCatching { URI(url) }.getOrNull()
    require(id.isNotBlank() && check.isNotBlank() && uri?.scheme == "https" && !uri.host.isNullOrBlank()) {
      "附件上传结果不完整，请重试"
    }
    return TopicAttachment(name, id, check, url, image)
  }

  fun uploadRequest(api: NgaApi, fid: Int, auth: String, name: String, mime: String, bytes: ByteArray, options: TopicUploadOptions = TopicUploadOptions()): NgaRequest {
    require(bytes.isNotEmpty() && bytes.size <= MAX_FILE_BYTES) { "附件不能为空，单个附件不能超过 20 MB" }
    val metadata = api.attachmentUploadMetadata(fid, auth, name)
    val boundary = "Yanga${UUID.randomUUID()}"
    val fields = metadata.bodyMap + mapOf(
      "origin_domain" to "bbs.nga.cn",
      "attachment_file1_auto_size" to options.compression,
      "attachment_file1_watermark" to options.watermark,
      "attachment_file1_dscp" to options.description,
      "attachment_file1_img" to if (mime.startsWith("image/")) "1" else "0",
    )
    val output = ByteArrayOutputStream()
    fun write(text: String) { output.write(text.toByteArray(Charsets.UTF_8)) }
    fields.forEach { (key, value) ->
      write("--$boundary\r\nContent-Disposition: form-data; name=\"$key\"\r\n\r\n$value\r\n")
    }
    val safeName = name.replace(Regex("[\\r\\n\\\"\\\\]"), "_")
    val safeMime = mime.takeIf { it.matches(Regex("[a-zA-Z0-9.+-]+/[a-zA-Z0-9.+-]+")) } ?: "application/octet-stream"
    write("--$boundary\r\nContent-Disposition: form-data; name=\"attachment_file1\"; filename=\"$safeName\"\r\nContent-Type: $safeMime\r\n\r\n")
    output.write(bytes)
    write("\r\n--$boundary--\r\n")
    return metadata.copy(headers = metadata.headers + ("Content-Type" to "multipart/form-data; boundary=$boundary"), binaryBody = output.toByteArray())
  }
}
