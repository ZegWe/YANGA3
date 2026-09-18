package com.yanga.client.web

import java.net.URI
import java.net.URLDecoder

/** Only reconstruct known login-authorization URLs; scanned data never supplies a request body. */
data class NgaQrLoginLink(val baseUrl: String, val key: String) {
  val url: String get() = "$baseUrl/nuke.php?__lib=login&__act=qrlogin_ui&qrkey=$key"

  companion object {
    private val hosts = setOf("bbs.nga.cn", "ngabbs.com", "nga.178.com", "bbs.ngacn.cc", "nga.donews.com")

    fun parse(raw: String): NgaQrLoginLink? = runCatching {
      val uri = URI(raw)
      val host = uri.host?.lowercase() ?: return null
      if (uri.scheme != "https" || host !in hosts || uri.rawUserInfo != null ||
        uri.port != -1 || uri.rawFragment != null || uri.rawPath != "/nuke.php") return null
      val pairs = uri.rawQuery.orEmpty().split('&').map {
        val pair = it.split('=', limit = 2)
        if (pair.size != 2) return null
        URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair[1], "UTF-8")
      }
      if (pairs.size != 3 || pairs.map { it.first }.toSet().size != 3) return null
      val params = pairs.toMap()
      if (params["__lib"] != "login" || params["__act"] != "qrlogin_ui") return null
      val key = params["qrkey"] ?: return null
      if (!key.matches(Regex("[a-zA-Z0-9]{1,512}"))) return null
      NgaQrLoginLink("https://$host", key)
    }.getOrNull()
  }
}
