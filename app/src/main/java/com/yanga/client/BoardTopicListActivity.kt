package com.yanga.client

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.yanga.client.ui.BoardContentViewModel
import com.yanga.client.ui.BoardDestination
import com.yanga.client.ui.BoardPreview
import com.yanga.client.ui.SubBoardOption
import com.yanga.client.ui.BoardTopicListScreen
import com.yanga.client.ui.BoardTopicListUiState
import com.yanga.client.ui.navigation.HomeActivityIntents
import com.yanga.client.ui.toData

class BoardTopicListActivity : YangaComposeActivity() {
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val destination = remember { HomeActivityIntents.boardDestination(intent) }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val boardContentViewModel =
      remember(app.repository, app.subBoardFilterStore) {
        BoardContentViewModel(app.repository, app.subBoardFilterStore)
      }
    val boardContentState by boardContentViewModel.state.collectAsState()
    val sessionData = loginSession?.toData()

    LaunchedEffect(destination, sessionData) {
      boardContentViewModel.openBoard(sessionData, destination)
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
      onBack = { backDispatcher?.onBackPressed() },
      onTopicClick = { topic ->
        context.startActivity(HomeActivityIntents.thread(context, topic))
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
        app.boardsCatalog.toggleLocalFavorite(board)
        boardContentViewModel.setFavorite(!boardState.isFavorite)
      },
      onSelectAllSubBoards = boardContentViewModel::selectAllSubBoards,
      onSetSubBoardEnabled = boardContentViewModel::setSubBoardEnabled,
      onOpenSubBoard = { subBoard -> openSubBoard(context, subBoard, boardState) },
      onTopicFilterChange = boardContentViewModel::setTopicFilter,
      onRefresh = boardContentViewModel::refresh,
      onLoadNextPage = boardContentViewModel::loadNextPage,
      modifier = Modifier.fillMaxSize(),
    )
  }

  private fun openSubBoard(
    context: android.content.Context,
    subBoard: SubBoardOption,
    parent: BoardTopicListUiState,
  ) {
    val destination =
      BoardDestination(
        id = subBoard.id,
        name = subBoard.name,
        iconUrl = parent.iconUrl,
        category = parent.category,
        isFavorite = false,
      )
    context.startActivity(HomeActivityIntents.boardTopics(context, destination))
  }

  companion object {
    const val EXTRA_BOARD_ID = "board_id"
    const val EXTRA_BOARD_NAME = "board_name"
    const val EXTRA_BOARD_ICON_URL = "board_icon_url"
    const val EXTRA_BOARD_CATEGORY = "board_category"
    const val EXTRA_BOARD_IS_FAVORITE = "board_is_favorite"
  }
}
