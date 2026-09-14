package com.yanga.client.ui.navigation

import androidx.compose.runtime.getValue

import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanga.client.ui.MainDestinationKey
import com.yanga.client.ui.LoginSessionUiState
import com.yanga.client.data.NgaReadOnlyRepository
import android.content.ActivityNotFoundException
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.yanga.client.ui.ThreadContentViewModel
import com.yanga.client.ui.PostAttachmentPreview
import com.yanga.client.ui.ThreadReadingScreen
import com.yanga.client.ui.ThreadDestination
import com.yanga.client.ui.ThreadUiState
import com.yanga.client.api.NgaStaticUrls
import com.yanga.client.data.image.ImageUrlResolver
import com.yanga.client.ui.navigation.NgaForumLinkParser
import com.yanga.client.ui.navigation.ThreadLinkRoute
import com.yanga.client.ui.toData

@Composable
fun ThreadRoute(
  destination: ThreadDestination,
  repository: NgaReadOnlyRepository,
  baseUrl: String,
  loginSession: LoginSessionUiState?,
  onBack: () -> Unit,
  navigate: (MainDestinationKey) -> Unit,
) {
  val context = LocalContext.current
  val threadContentViewModel = viewModel<ThreadContentViewModel> { ThreadContentViewModel(repository) }
  val threadContentState by threadContentViewModel.state.collectAsState()

  LaunchedEffect(destination.id, destination.page, loginSession?.cookie) {
    threadContentViewModel.ensureThreadOpened(loginSession?.toData(), destination)
  }

  val threadState =
    threadContentState?.takeIf { threadContentViewModel.matchesThread(destination.id) }
      ?: ThreadUiState(title = destination.title)
  fun openReply(post: com.yanga.client.ui.PostPreview?) {
    val url = Uri.parse(baseUrl).buildUpon().encodedPath("/post.php").clearQuery()
      .appendQueryParameter("action", "reply").appendQueryParameter("tid", destination.id)
      .appendQueryParameter("pid", post?.pid?.ifBlank { "0" } ?: "0").build().toString()
    navigate(MainDestinationKey.Web(url = url, title = post?.let { "回复 ${it.author}" } ?: "回复主题", baseUrl = baseUrl))
  }
  ThreadReadingScreen(
    state = threadState,
    onBack = onBack,
    onReplyClick = { openReply(null) },
    onReplyPost = { openReply(it) },
    onFilterAuthor = { threadContentViewModel.filterAuthor(loginSession?.toData(), it) },
    onReact = if (loginSession == null) null else { post, support ->
      repository.reactToPost(loginSession.toData(), destination.id, post.pid.ifBlank { "0" }, support).fold(
        onSuccess = {
          val fresh = if (post.floorNumber == 0) repository.loadThread(loginSession.toData(), destination.id, 1)
            .getOrNull()?.posts?.firstOrNull { it.lou == 0 }
            else repository.loadThreadPost(loginSession.toData(), post.pid).getOrNull()
          fresh?.let { threadContentViewModel.updatePostScore(post.pid, it.score) }
          Result.success(fresh?.score)
        },
        onFailure = { Result.failure(it) },
      )
    },
    onPageChange = { page ->
      threadContentViewModel.openPage(loginSession?.toData(), page)
    },
    onFloorJump = { floor ->
      threadContentViewModel.openFloor(loginSession?.toData(), floor)
    },
    onOpenInBrowser = {
      val page = threadState.page.toIntOrNull() ?: 1
      val url =
        NgaStaticUrls.threadReadUrl(
          baseUrl = baseUrl,
          tid = destination.id,
          page = page,
        )
      navigate(
        MainDestinationKey.Web(
          url = url,
          title = threadState.title.ifBlank { destination.title },
          baseUrl = baseUrl,
        ),
      )
    },
    onLinkClick = { url ->
      when (val route = NgaForumLinkParser.threadLinkRoute(url, currentTid = destination.id)) {
        is ThreadLinkRoute.Post -> {
          if (!route.threadId.isNullOrBlank() && route.threadId != destination.id) {
            navigate(
              MainDestinationKey.Thread(
                destination =
                  ThreadDestination(
                    id = route.threadId,
                    title = "",
                    page = route.page ?: 1,
                    targetPostId = route.postId,
                  ),
              ),
            )
          } else if (route.page != null) {
            threadContentViewModel.openPostOnPage(loginSession?.toData(), route.postId, route.page)
          } else {
            threadContentViewModel.openPost(loginSession?.toData(), route.postId)
          }
        }
        is ThreadLinkRoute.CurrentThreadPage -> {
          threadContentViewModel.openPage(loginSession?.toData(), route.page)
        }
        is ThreadLinkRoute.OtherThread -> {
          navigate(
            MainDestinationKey.Thread(
              destination =
                ThreadDestination(
                  id = route.destination.tid,
                  title = "",
                  page = route.destination.page,
                ),
            ),
          )
        }
        null -> {
          openPostLink(context, url)
        }
      }
    },
    onAttachmentDownload = { downloadAttachment(context, it) },
    onVote = if (loginSession == null) null else { poll, ids ->
      val session = loginSession.toData()
      repository.submitPoll(session, poll, ids).fold(
        onSuccess = {
          // A successful vote must not be retried just because reloading results failed.
          val fresh = repository.loadThread(session, poll.tid, threadState.page.toIntOrNull() ?: 1)
            .getOrNull()?.posts?.firstNotNullOfOrNull { it.poll }
          fresh?.let(threadContentViewModel::updatePollResults)
          Result.success(fresh)
        },
        onFailure = { Result.failure(it) },
      )
    },
    modifier = Modifier.fillMaxSize(),
  )
}

private fun downloadAttachment(context: Context, attachment: PostAttachmentPreview) {
  val uri = runCatching { Uri.parse(attachment.url) }.getOrNull()
  if (uri == null || uri.scheme.isNullOrBlank()) {
    Toast.makeText(context, "附件链接无效", Toast.LENGTH_SHORT).show()
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

  val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
  downloadManager.enqueue(request)
  Toast.makeText(context, "已开始下载：$fileName", Toast.LENGTH_SHORT).show()
}

private fun safeDownloadFileName(attachment: PostAttachmentPreview): String {
  val candidate = attachment.name.ifBlank { ImageUrlResolver.fileName(attachment.url) }
  return candidate
    .replace(Regex("""[\\/:*?"<>|]"""), "_")
    .trim()
    .ifBlank { "yanga-attachment" }
}

private fun openPostLink(context: Context, url: String) {
  try {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
  } catch (_: ActivityNotFoundException) {
    // No external handler is available for this link.
  }
}
