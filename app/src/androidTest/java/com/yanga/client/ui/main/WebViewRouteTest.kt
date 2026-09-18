package com.yanga.client.ui

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.yanga.client.ui.navigation.WebViewRoute
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Rule
import org.junit.Test

class WebViewRouteTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun percentageHeightLoginFrameHasVisibleContent() {
    // Match NGA's document/body/iframe percentage-height chain without network access.
    val html = """
      <!doctype html><html style="height:100%"><head>
      <meta name="viewport" content="width=device-width, initial-scale=1">
      </head><body style="height:100%;margin:0">
      <iframe id="login" style="height:99.5%;border:0" srcdoc="<input placeholder='Username'>"></iframe>
      </body></html>
    """.trimIndent()
    compose.setContent {
      WebViewRoute("data:text/html;charset=utf-8,${Uri.encode(html)}", "Login", "https://bbs.nga.cn", "", {})
    }
    val visible = AtomicBoolean(false)
    compose.waitUntil(timeoutMillis = 15_000) {
      compose.runOnIdle {
        findWebView(compose.activity.window.decorView)?.evaluateJavascript(
          "document.readyState === 'complete' && document.getElementById('login').getBoundingClientRect().height > 100",
        ) { visible.set(it == "true") }
      }
      visible.get()
    }
  }

  private fun findWebView(view: View): WebView? {
    if (view is WebView) return view
    if (view is ViewGroup) {
      for (index in 0 until view.childCount) {
        findWebView(view.getChildAt(index))?.let { return it }
      }
    }
    return null
  }
}
