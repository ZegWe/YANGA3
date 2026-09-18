package com.yanga.client.web

import android.net.Uri
import android.webkit.CookieManager

object NgaWebViewCookies {
  /** Load authorization pages only after all account cookies have reached WebView. */
  fun syncAuthorizationSession(cookieManager: CookieManager, cookieHeader: String, baseUrl: String, onComplete: (Boolean) -> Unit) {
    val cookies = cookieHeader.split(';').map(String::trim).filter { it.contains('=') }
    if (cookies.isEmpty()) { onComplete(false); return }
    fun next(index: Int) {
      if (index == cookies.size) {
        cookieManager.flush()
        onComplete(true)
        return
      }
      cookieManager.setCookie(baseUrl, "${cookies[index]}; Path=/; Secure") { accepted ->
        if (accepted) next(index + 1) else onComplete(false)
      }
    }
    cookieManager.setAcceptCookie(true)
    next(0)
  }

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
