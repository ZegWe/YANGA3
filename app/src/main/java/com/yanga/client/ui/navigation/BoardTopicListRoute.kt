package com.yanga.client.ui.navigation

import androidx.compose.runtime.getValue

import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanga.client.ui.MainDestinationKey
import com.yanga.client.ui.LoginSessionUiState
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.YangaApplication
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.yanga.client.ui.BoardContentViewModel
import com.yanga.client.ui.BoardDestination
import com.yanga.client.ui.BoardPreview
import com.yanga.client.ui.BoardTopicListScreen
import com.yanga.client.ui.BoardTopicListUiState
import com.yanga.client.ui.navigation.toNavigationKey
import com.yanga.client.ui.toData

@Composable
fun BoardTopicListRoute(
  destination: BoardDestination,
  repository: NgaReadOnlyRepository,
  app: YangaApplication?,
  loginSession: LoginSessionUiState?,
  onBack: () -> Unit,
  navigate: (MainDestinationKey) -> Unit,
  baseUrl: String = "https://bbs.nga.cn",
) {
  val boardContentViewModel =
    viewModel<BoardContentViewModel> {
      BoardContentViewModel(repository, app?.subBoardFilterStore)
    }
  val boardContentState by boardContentViewModel.state.collectAsState()
  val sessionData = loginSession?.toData()

  LaunchedEffect(destination, sessionData) {
    boardContentViewModel.ensureBoardOpened(sessionData, destination)
  }

  val boardState =
    boardContentState?.takeIf { it.fid == destination.id }
      ?: BoardTopicListUiState(
        boardName = destination.name,
        fid = destination.id,
        iconUrl = destination.iconUrl,
        category = destination.category,
        isFavorite = destination.isFavorite,
      )
  val context = androidx.compose.ui.platform.LocalContext.current
  val topicComposer = viewModel<com.yanga.client.ui.TopicComposerViewModel>()
  LaunchedEffect(topicComposer.visible, loginSession?.uid, baseUrl) {
    if (topicComposer.visible) {
      topicComposer.bindDraft(context, loginSession?.uid ?: "guest", destination.id.toIntOrNull() ?: 0)
      if (!loginSession?.cookie.isNullOrBlank()) topicComposer.prepare(repository, sessionData, destination.id.toIntOrNull() ?: 0)
    }
  }
  LaunchedEffect(topicComposer.sent) {
    if (topicComposer.sent) {
      topicComposer.sent = false
      boardContentViewModel.refresh()
      android.widget.Toast.makeText(context, "发布成功", android.widget.Toast.LENGTH_SHORT).show()
    }
  }
  if (topicComposer.visible) com.yanga.client.ui.TopicComposer(
    boardName = destination.name, fid = destination.id.toIntOrNull() ?: 0,
    model = topicComposer, repository = repository, loginSession = loginSession,
    onLogin = { topicComposer.close(); navigate(MainDestinationKey.Login) },
    onWeb = {
      topicComposer.close()
      val url = android.net.Uri.parse(baseUrl).buildUpon().encodedPath("/post.php").clearQuery()
        .appendQueryParameter("action", "new").appendQueryParameter("fid", destination.id).build().toString()
      navigate(MainDestinationKey.Web(url, "发帖 · ${destination.name}", baseUrl))
    },
  )
  BoardTopicListScreen(
    state = boardState,
    onBack = onBack,
    onTopicClick = { topic ->
      navigate(topic.toNavigationKey())
    },
    onToggleFavorite = {
      val board =
        BoardPreview(
          id = boardState.fid,
          name = boardState.boardName,
          metadata = "fid: ${boardState.fid}",
          marker = boardState.boardName.take(1),
          iconUrl = boardState.iconUrl,
          category = boardState.category,
          isFavorite = boardState.isFavorite,
        )
      app?.boardsCatalog?.toggleLocalFavorite(board)
      boardContentViewModel.setFavorite(!boardState.isFavorite)
    },
    onSelectAllSubBoards = boardContentViewModel::selectAllSubBoards,
    onSetSubBoardEnabled = boardContentViewModel::setSubBoardEnabled,
    onOpenSubBoard = { subBoard ->
      navigate(MainDestinationKey.Board(BoardDestination(
        id = subBoard.id, name = subBoard.name, iconUrl = boardState.iconUrl, category = boardState.category,
      )))
    },
    onTopicFilterChange = boardContentViewModel::setTopicFilter,
    onSearchClick = {
      navigate(MainDestinationKey.Search(destination))
    },
    onCreateTopic = topicComposer::open,
    onRefresh = boardContentViewModel::refresh,
    onLoadNextPage = boardContentViewModel::loadNextPage,
    modifier = Modifier.fillMaxSize(),
  )
}
