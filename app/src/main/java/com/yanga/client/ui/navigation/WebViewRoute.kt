package com.yanga.client.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.runtime.saveable.Saver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.saveable.rememberSaveable
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yanga.client.web.NgaWebViewConfigurator
import com.yanga.client.api.NgaLoginCookies
import com.yanga.client.web.NgaWebViewCookies
import com.yanga.client.web.WebViewLoadState
import com.yanga.client.web.destroySafely

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WebViewRoute(
  pageUrl: String,
  fallbackTitle: String,
  cookieBaseUrl: String,
  cookieHeader: String,
  onBack: () -> Unit,
  onLoginCookies: ((String) -> Boolean)? = null,
  authorizationOnly: Boolean = false,
) {
  var webView by remember { mutableStateOf<WebView?>(null) }
  var canGoBack by remember { mutableStateOf(false) }
  val savedWebState = rememberSaveable(
    saver = Saver<Bundle, Bundle>(
      save = { state -> webView?.saveState(state); state },
      restore = { it },
    ),
  ) { Bundle() }
  val lifecycleOwner = LocalLifecycleOwner.current
  val loginCallback by rememberUpdatedState(onLoginCookies)
  val snackbar = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var loginCompleted by remember { mutableStateOf(false) }
  val initialSession = remember {
    runCatching { NgaLoginCookies.parse(CookieManager.getInstance().getCookie(cookieBaseUrl).orEmpty()) }.getOrNull()
  }
  fun completeLogin(): Boolean {
    if (loginCompleted) return true
    val cookies = CookieManager.getInstance().getCookie(cookieBaseUrl).orEmpty()
    return (loginCallback?.invoke(cookies) == true).also { loginCompleted = it }
  }
  LaunchedEffect(lifecycleOwner, onLoginCookies != null) {
    if (loginCallback == null) return@LaunchedEffect
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      while (!loginCompleted) {
        val cookies = CookieManager.getInstance().getCookie(cookieBaseUrl).orEmpty()
        val session = runCatching { NgaLoginCookies.parse(cookies) }.getOrNull()
        if (session != null && session != initialSession) completeLogin()
        delay(750)
      }
    }
  }
  DisposableEffect(lifecycleOwner, webView) {
    val view = webView
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> view?.onResume()
        Lifecycle.Event.ON_PAUSE -> view?.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
  fun handleBack() {
    val view = webView
    if (view?.canGoBack() == true) view.goBack() else onBack()
  }
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
    mutableStateOf(fallbackTitle.ifBlank { "NGA" })
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

  // Let NavDisplay handle predictive back to the parent when web history is empty.
  BackHandler(enabled = canGoBack) { handleBack() }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(snackbar) },
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = displayTitle,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
          )
        },
        navigationIcon = {
          IconButton(onClick = { handleBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (authorizationOnly) {
            TextButton(onClick = onBack) { Text("关闭") }
          }
          if (onLoginCookies != null) {
            TextButton(onClick = {
              if (!completeLogin()) scope.launch {
                snackbar.showSnackbar("尚未检测到登录状态，请先在网页中完成登录和验证")
              }
            }) { Text("完成登录") }
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
              // WebView uses its LayoutParams to size the CSS viewport. WRAP_CONTENT
              // collapses percentage-height pages such as NGA's login iframe to zero.
              view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
              )
              webView = view
              CookieManager.getInstance().apply {
                setAcceptCookie(true)
                if (cookieHeader.isNotBlank() && !authorizationOnly) {
                  NgaWebViewCookies.syncSession(this, cookieHeader, cookieBaseUrl)
                }
              }
              NgaWebViewConfigurator.configure(view)
              view.webViewClient =
                object : WebViewClient() {
                  override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    if (!authorizationOnly) return false
                    val uri = request?.url ?: return true
                    if (uri.toString() == "about:blank") return false
                    // The official page tries to open its own app; keep authorization here.
                    if (uri.scheme == "nga") return true
                    val allowed = uri.scheme == "https" && uri.host == android.net.Uri.parse(cookieBaseUrl).host &&
                      uri.port == -1 && uri.userInfo == null
                    if (!allowed && request.isForMainFrame) scope.launch {
                      snackbar.showSnackbar("请在当前 NGA 页面确认授权")
                    }
                    return !allowed
                  }

                  override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    canGoBack = view?.canGoBack() == true
                  }

                  override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    loadTracker.pendingError = null
                    onLoadStateChange(WebViewLoadState.Loading)
                  }

                  override fun onPageFinished(view: WebView?, url: String?) {
                    canGoBack = view?.canGoBack() == true
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
                    if (!title.isNullOrBlank() && !authorizationOnly) {
                      onTitleChange(title)
                    }
                  }
                }
              if (authorizationOnly) {
                NgaWebViewCookies.syncAuthorizationSession(CookieManager.getInstance(), cookieHeader, cookieBaseUrl) { success ->
                  if (webView === view) {
                    if (success) view.loadUrl(pageUrl)
                    else onLoadStateChange(WebViewLoadState.Error("同步登录状态失败，请返回后重新登录"))
                  }
                }
              } else if (view.restoreState(savedWebState) == null) view.loadUrl(pageUrl)
              canGoBack = view.canGoBack()
            }
          },
          onRelease = { view ->
            view.saveState(savedWebState)
            webView = null
            view.destroySafely()
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


private class WebViewLoadTracker {
  var pendingError: String? = null
}
