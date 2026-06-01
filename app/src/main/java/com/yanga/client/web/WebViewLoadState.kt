package com.yanga.client.web

internal sealed class WebViewLoadState {
  data object Loading : WebViewLoadState()

  data object Ready : WebViewLoadState()

  data class Error(val message: String) : WebViewLoadState()
}
