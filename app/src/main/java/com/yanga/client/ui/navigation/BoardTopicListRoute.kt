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
    onRefresh = boardContentViewModel::refresh,
    onLoadNextPage = boardContentViewModel::loadNextPage,
    modifier = Modifier.fillMaxSize(),
  )
}
