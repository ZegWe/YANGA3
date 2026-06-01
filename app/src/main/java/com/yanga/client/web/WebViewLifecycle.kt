package com.yanga.client.web

import android.view.ViewGroup
import android.webkit.WebView

internal fun WebView.prepareForExit() {
  stopLoading()
  loadUrl("about:blank")
  clearHistory()
}

internal fun WebView.destroySafely() {
  prepareForExit()
  removeAllViews()
  (parent as? ViewGroup)?.removeView(this)
  destroy()
}
