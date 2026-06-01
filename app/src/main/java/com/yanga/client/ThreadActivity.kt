package com.yanga.client

import android.content.ActivityNotFoundException
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.yanga.client.ui.ThreadContentViewModel
import com.yanga.client.ui.PostAttachmentPreview
import com.yanga.client.ui.ThreadReadingScreen
import com.yanga.client.ui.ThreadUiState
import com.yanga.client.api.NgaStaticUrls
import com.yanga.client.data.image.ImageUrlResolver
import com.yanga.client.ui.navigation.HomeActivityIntents
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
      onLinkClick = { url -> openPostLink(url) },
      onAttachmentDownload = ::downloadAttachment,
      modifier = Modifier.fillMaxSize(),
    )
  }

  private fun downloadAttachment(attachment: PostAttachmentPreview) {
    val uri = runCatching { Uri.parse(attachment.url) }.getOrNull()
    if (uri == null || uri.scheme.isNullOrBlank()) {
      Toast.makeText(this, "附件链接无效", Toast.LENGTH_SHORT).show()
      return
    }

    val fileName = safeDownloadFileName(attachment)
    val request =
      DownloadManager.Request(uri)
        .setTitle(fileName)
        .setDescription("Yanga 附件下载")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request)
    Toast.makeText(this, "已开始下载：$fileName", Toast.LENGTH_SHORT).show()
  }

  private fun safeDownloadFileName(attachment: PostAttachmentPreview): String {
    val candidate = attachment.name.ifBlank { ImageUrlResolver.fileName(attachment.url) }
    return candidate
      .replace(Regex("""[\\/:*?"<>|]"""), "_")
      .trim()
      .ifBlank { "yanga-attachment" }
  }

  private fun openPostLink(url: String) {
    try {
      startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
      // No external handler is available for this link.
    }
  }

  companion object {
    const val EXTRA_THREAD_ID = "thread_id"
    const val EXTRA_THREAD_TITLE = "thread_title"
    const val EXTRA_THREAD_PAGE = "thread_page"
  }
}
