package com.yanga.client.web

import android.view.View
import android.webkit.WebView

object NgaWebViewConfigurator {
  fun configure(view: WebView) {
    view.settings.apply {
      javaScriptEnabled = true
      domStorageEnabled = true
      useWideViewPort = true
      loadWithOverviewMode = true
      builtInZoomControls = false
      displayZoomControls = false
      setSupportZoom(false)
    }
    view.isHorizontalScrollBarEnabled = false
    view.overScrollMode = View.OVER_SCROLL_NEVER
  }

  fun applyMobileViewportFix(view: WebView) {
    view.evaluateJavascript(OVERFLOW_FIX_JS, null)
  }

  private const val OVERFLOW_FIX_JS =
    """
    (function() {
      var style = document.getElementById('yanga-webview-fit');
      if (!style) {
        style = document.createElement('style');
        style.id = 'yanga-webview-fit';
        style.textContent = 'html, body { max-width: 100% !important; overflow-x: hidden !important; }';
        document.head.appendChild(style);
      }
      window.scrollTo(0, window.scrollY || 0);
    })();
    """
}
