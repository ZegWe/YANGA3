package com.yanga.client.web

import android.net.Uri
import android.webkit.CookieManager

object NgaWebViewCookies {
  fun syncSession(cookieManager: CookieManager, cookieHeader: String, baseUrl: String) {
    if (cookieHeader.isBlank()) return

    cookieManager.setAcceptCookie(true)
    val normalized = baseUrl.trimEnd('/')
    val uri = Uri.parse(normalized)
    val host = uri.host ?: return
    val scheme = uri.scheme ?: "https"
    val targetUrls =
      listOf(
        "$scheme://$host",
        if (scheme == "https") "http://$host" else "https://$host",
      ).distinct()

    cookieHeader
      .split(';')
      .map { it.trim() }
      .filter { it.isNotBlank() && it.contains('=') }
      .forEach { cookiePair ->
        targetUrls.forEach { url ->
          cookieManager.setCookie(url, cookiePair)
        }
      }
    cookieManager.flush()
  }
}
