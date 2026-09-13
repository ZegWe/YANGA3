package com.yanga.client.ui.navigation

import com.yanga.client.ui.BoardDestination
import com.yanga.client.ui.BoardPreview
import com.yanga.client.ui.MainDestinationKey
import com.yanga.client.ui.ThreadDestination
import com.yanga.client.ui.TopicNavigationTarget
import com.yanga.client.ui.TopicPreview

fun BoardPreview.toNavigationKey() = MainDestinationKey.Board(
  BoardDestination(id, name, iconUrl, category, isFavorite),
)

fun TopicPreview.toNavigationKey(): MainDestinationKey = when (val target = navigationTarget) {
  is TopicNavigationTarget.Board -> MainDestinationKey.Board(target.destination)
  TopicNavigationTarget.Thread -> MainDestinationKey.Thread(ThreadDestination(id, title))
}

fun threadDeepLink(rawUrl: String?): MainDestinationKey.Thread? {
  val thread = NgaForumLinkParser.threadDestination(rawUrl)
  val post = NgaForumLinkParser.postDestination(rawUrl)
  val tid = thread?.tid ?: post?.threadId
  if (tid.isNullOrBlank() && post == null) return null
  return MainDestinationKey.Thread(ThreadDestination(
    id = tid.orEmpty(),
    title = "",
    page = thread?.page ?: post?.page ?: 1,
    targetPostId = post?.postId,
  ))
}
