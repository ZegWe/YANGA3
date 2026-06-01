package com.yanga.client

import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yanga.client.web.NgaWebViewConfigurator
import com.yanga.client.web.NgaWebViewCookies
import com.yanga.client.web.WebViewLoadState
import com.yanga.client.web.destroySafely
import com.yanga.client.web.prepareForExit

class WebViewActivity : YangaComposeActivity() {
  private var webView: WebView? = null
  private var webViewReleased = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      webViewReleased = savedInstanceState.getBoolean(STATE_WEBVIEW_RELEASED, false)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(STATE_WEBVIEW_RELEASED, webViewReleased)
  }

  override fun onPause() {
    webView?.onPause()
    webView?.pauseTimers()
    super.onPause()
  }

  override fun onResume() {
    super.onResume()
    webView?.onResume()
    webView?.resumeTimers()
  }

  override fun onStop() {
    if (isFinishing) {
      webView?.prepareForExit()
    }
    super.onStop()
  }

  override fun onDestroy() {
    val pendingDestroy = webView
    webView = null
    super.onDestroy()
    if (webViewReleased || pendingDestroy == null) return
    webViewReleased = true
    pendingDestroy.post {
      pendingDestroy.destroySafely()
    }
  }

  private fun handleBack() {
    val view = webView
    if (view != null && view.canGoBack()) {
      view.goBack()
      return
    }
    view?.prepareForExit()
    finish()
  }

  @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  override fun Content() {
    val pageUrl = remember { intent.getStringExtra(EXTRA_URL).orEmpty() }
    val fallbackTitle = remember { intent.getStringExtra(EXTRA_TITLE).orEmpty() }
    val cookieBaseUrl =
      remember {
        intent.getStringExtra(EXTRA_BASE_URL)?.takeIf { it.isNotBlank() }
          ?: app.repository.currentBaseUrl()
      }
    val cookieHeader = remember { loginSession?.cookie.orEmpty() }

    var loadState by remember(pageUrl) {
      mutableStateOf(
        if (pageUrl.isBlank()) {
          WebViewLoadState.Error("缺少页面地址")
        } else {
          WebViewLoadState.Loading
        },
      )
    }
    var displayTitle by remember(fallbackTitle) {
      mutableStateOf(fallbackTitle.ifBlank { DEFAULT_TITLE })
    }
    val loadTracker = remember { WebViewLoadTracker() }

    val onLoadStateChange by rememberUpdatedState<(WebViewLoadState) -> Unit>({ loadState = it })
    val onTitleChange by rememberUpdatedState<(String) -> Unit>({ title ->
      if (title.isNotBlank()) {
        displayTitle = title
      }
    })

    fun reloadPage() {
      if (pageUrl.isBlank()) return
      loadTracker.pendingError = null
      loadState = WebViewLoadState.Loading
      webView?.loadUrl(pageUrl)
    }

    BackHandler { handleBack() }

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = displayTitle,
              maxLines = 1,
            )
          },
          navigationIcon = {
            IconButton(onClick = { handleBack() }) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          },
        )
      },
    ) { paddingValues ->
      Box(
        modifier =
          Modifier
            .padding(paddingValues)
            .fillMaxSize(),
      ) {
        if (pageUrl.isNotBlank()) {
          AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
              WebView(context).also { view ->
                webView = view
                CookieManager.getInstance().apply {
                  setAcceptCookie(true)
                  if (cookieHeader.isNotBlank()) {
                    NgaWebViewCookies.syncSession(this, cookieHeader, cookieBaseUrl)
                  }
                }
                NgaWebViewConfigurator.configure(view)
                view.webViewClient =
                  object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                      loadTracker.pendingError = null
                      onLoadStateChange(WebViewLoadState.Loading)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                      view?.let(NgaWebViewConfigurator::applyMobileViewportFix)
                      val errorMessage = loadTracker.pendingError
                      onLoadStateChange(
                        if (errorMessage != null) {
                          WebViewLoadState.Error(errorMessage)
                        } else {
                          WebViewLoadState.Ready
                        },
                      )
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onReceivedError(
                      view: WebView?,
                      errorCode: Int,
                      description: String?,
                      failingUrl: String?,
                    ) {
                      if (failingUrl == pageUrl) {
                        loadTracker.pendingError = description?.ifBlank { null } ?: "页面加载失败"
                      }
                    }

                    override fun onReceivedError(
                      view: WebView?,
                      request: WebResourceRequest?,
                      error: WebResourceError?,
                    ) {
                      if (request?.isForMainFrame == true) {
                        loadTracker.pendingError =
                          error?.description?.toString()?.ifBlank { null }
                            ?: "页面加载失败"
                      }
                    }
                  }
                view.webChromeClient =
                  object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                      if (!title.isNullOrBlank()) {
                        onTitleChange(title)
                      }
                    }
                  }
                view.loadUrl(pageUrl)
              }
            },
            onRelease = { view ->
              view.prepareForExit()
            },
          )
        }

        when (val state = loadState) {
          WebViewLoadState.Loading -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            LoadingIndicator(
              modifier =
                Modifier
                  .align(Alignment.Center)
                  .size(64.dp),
            )
          }
          is WebViewLoadState.Error -> {
            Column(
              modifier =
                Modifier
                  .align(Alignment.Center)
                  .padding(horizontal = 24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
              )
              if (pageUrl.isNotBlank()) {
                Button(onClick = { reloadPage() }) {
                  Text("重试")
                }
              }
            }
          }
          WebViewLoadState.Ready -> Unit
        }
      }
    }
  }

  companion object {
    const val EXTRA_URL = "url"
    const val EXTRA_TITLE = "title"
    const val EXTRA_BASE_URL = "base_url"
    private const val DEFAULT_TITLE = "NGA"
    private const val STATE_WEBVIEW_RELEASED = "webview_released"
  }

  private class WebViewLoadTracker {
    var pendingError: String? = null
  }
}
