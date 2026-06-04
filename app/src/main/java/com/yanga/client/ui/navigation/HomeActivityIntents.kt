package com.yanga.client.ui.navigation

import android.content.Context
import android.content.Intent
import com.yanga.client.BoardTopicListActivity
import com.yanga.client.LoginActivity
import com.yanga.client.ThreadActivity
import com.yanga.client.WebViewActivity
import com.yanga.client.ui.BoardDestination
import com.yanga.client.ui.BoardPreview
import com.yanga.client.ui.ThreadDestination
import com.yanga.client.ui.TopicPreview

object HomeActivityIntents {
  fun login(context: Context): Intent = Intent(context, LoginActivity::class.java)

  fun boardTopics(context: Context, board: BoardPreview): Intent =
    boardTopics(
      context = context,
      destination =
        BoardDestination(
          id = board.id,
          name = board.name,
          iconUrl = board.iconUrl,
          category = board.category,
          isFavorite = board.isFavorite,
        ),
    )

  fun boardTopics(context: Context, destination: BoardDestination): Intent =
    Intent(context, BoardTopicListActivity::class.java).apply {
      putExtra(BoardTopicListActivity.EXTRA_BOARD_ID, destination.id)
      putExtra(BoardTopicListActivity.EXTRA_BOARD_NAME, destination.name)
      putExtra(BoardTopicListActivity.EXTRA_BOARD_ICON_URL, destination.iconUrl)
      putExtra(BoardTopicListActivity.EXTRA_BOARD_CATEGORY, destination.category)
      putExtra(BoardTopicListActivity.EXTRA_BOARD_IS_FAVORITE, destination.isFavorite)
    }

  fun thread(context: Context, topic: TopicPreview): Intent =
    thread(
      context = context,
      destination = ThreadDestination(id = topic.id, title = topic.title),
    )

  fun thread(context: Context, destination: ThreadDestination): Intent =
    Intent(context, ThreadActivity::class.java).apply {
      putExtra(ThreadActivity.EXTRA_THREAD_ID, destination.id)
      putExtra(ThreadActivity.EXTRA_THREAD_TITLE, destination.title)
      putExtra(ThreadActivity.EXTRA_THREAD_PAGE, destination.page)
      destination.targetPostId?.let { putExtra(ThreadActivity.EXTRA_TARGET_POST_ID, it) }
      destination.targetFloorNumber?.let { putExtra(ThreadActivity.EXTRA_TARGET_FLOOR_NUMBER, it) }
    }

  fun webView(
    context: Context,
    url: String,
    title: String = "",
    baseUrl: String,
  ): Intent =
    Intent(context, WebViewActivity::class.java).apply {
      putExtra(WebViewActivity.EXTRA_URL, url)
      putExtra(WebViewActivity.EXTRA_TITLE, title)
      putExtra(WebViewActivity.EXTRA_BASE_URL, baseUrl)
    }

  fun boardDestination(intent: Intent): BoardDestination =
    BoardDestination(
      id = intent.getStringExtra(BoardTopicListActivity.EXTRA_BOARD_ID).orEmpty(),
      name = intent.getStringExtra(BoardTopicListActivity.EXTRA_BOARD_NAME).orEmpty(),
      iconUrl = intent.getStringExtra(BoardTopicListActivity.EXTRA_BOARD_ICON_URL),
      category = intent.getStringExtra(BoardTopicListActivity.EXTRA_BOARD_CATEGORY).orEmpty(),
      isFavorite = intent.getBooleanExtra(BoardTopicListActivity.EXTRA_BOARD_IS_FAVORITE, false),
    )

  fun threadDestination(intent: Intent): ThreadDestination {
    val deepLinkDestination = NgaForumLinkParser.threadDestination(intent.dataString)
    val deepLinkPostDestination = NgaForumLinkParser.postDestination(intent.dataString)
    return ThreadDestination(
      id = intent.getStringExtra(ThreadActivity.EXTRA_THREAD_ID).orEmpty().ifBlank {
        deepLinkDestination?.tid ?: deepLinkPostDestination?.threadId.orEmpty()
      },
      title = intent.getStringExtra(ThreadActivity.EXTRA_THREAD_TITLE).orEmpty(),
      page =
        intent.getIntExtra(
          ThreadActivity.EXTRA_THREAD_PAGE,
          deepLinkDestination?.page ?: deepLinkPostDestination?.page ?: 1,
        ),
      targetPostId = intent.getStringExtra(ThreadActivity.EXTRA_TARGET_POST_ID) ?: deepLinkPostDestination?.postId,
      targetFloorNumber =
        intent
          .takeIf { it.hasExtra(ThreadActivity.EXTRA_TARGET_FLOOR_NUMBER) }
          ?.getIntExtra(ThreadActivity.EXTRA_TARGET_FLOOR_NUMBER, 0),
    )
  }
}
