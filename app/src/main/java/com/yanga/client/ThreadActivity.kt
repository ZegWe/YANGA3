package com.yanga.client

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.yanga.client.ui.ThreadContentViewModel
import com.yanga.client.ui.ThreadReadingScreen
import com.yanga.client.ui.ThreadUiState
import com.yanga.client.api.NgaStaticUrls
import com.yanga.client.ui.navigation.HomeActivityIntents
import com.yanga.client.ui.navigation.NgaForumLinkParser
import com.yanga.client.ui.toData

class ThreadActivity : YangaComposeActivity() {
  @Composable
  override fun Content() {
    val destination = remember { HomeActivityIntents.threadDestination(intent) }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val threadContentViewModel = remember(app.repository) { ThreadContentViewModel(app.repository) }
    val threadContentState by threadContentViewModel.state.collectAsState()

    LaunchedEffect(destination.id, destination.page, loginSession?.cookie) {
      threadContentViewModel.openThread(loginSession?.toData(), destination)
    }

    val threadState =
      threadContentState?.takeIf { threadContentViewModel.matchesThread(destination.id) }
        ?: ThreadUiState(title = destination.title)
    ThreadReadingScreen(
      state = threadState,
      onBack = { backDispatcher?.onBackPressed() },
      onOpenInBrowser = {
        val page = threadState.page.toIntOrNull() ?: 1
        val baseUrl = app.repository.currentBaseUrl()
        val url =
          NgaStaticUrls.threadReadUrl(
            baseUrl = baseUrl,
            tid = destination.id,
            page = page,
          )
        startActivity(
          HomeActivityIntents.webView(
            context = this@ThreadActivity,
            url = url,
            title = threadState.title.ifBlank { destination.title },
            baseUrl = baseUrl,
          ),
        )
      },
      onLinkClick = { url -> openPostLink(url, threadState.title.ifBlank { destination.title }) },
      modifier = Modifier.fillMaxSize(),
    )
  }

  private fun openPostLink(url: String, title: String) {
    if (NgaForumLinkParser.isForumUrl(url)) {
      runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
      }.onFailure { error ->
        if (error !is ActivityNotFoundException) throw error
        openWebViewLink(url, title)
      }
      return
    }

    openWebViewLink(url, title)
  }

  private fun openWebViewLink(url: String, title: String) {
    startActivity(
      HomeActivityIntents.webView(
        context = this,
        url = url,
        title = title,
        baseUrl = app.repository.currentBaseUrl(),
      ),
    )
  }

  companion object {
    const val EXTRA_THREAD_ID = "thread_id"
    const val EXTRA_THREAD_TITLE = "thread_title"
    const val EXTRA_THREAD_PAGE = "thread_page"
  }
}
