package com.yanga.client.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.data.boards.BoardsCatalog
import com.yanga.client.ui.BoardPreview
import com.yanga.client.ui.BoardsListViewModel
import com.yanga.client.ui.BoardsScreen
import com.yanga.client.ui.LoginSessionUiState
import com.yanga.client.ui.toData

@Composable
fun BoardListRoute(
  loginSession: LoginSessionUiState?,
  repository: NgaReadOnlyRepository,
  boardsCatalog: BoardsCatalog?,
  forumEndpoint: String,
  onBoardClick: (BoardPreview) -> Unit,
  onSearchClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val boardsViewModel = viewModel<BoardsListViewModel> {
    BoardsListViewModel(repository, boardsCatalog)
  }
  val boardsState by boardsViewModel.state.collectAsState()
  LaunchedEffect(loginSession, forumEndpoint) {
    boardsViewModel.ensureLoaded(loginSession?.toData(), forumEndpoint)
  }
  // Do not measure a restored pager/grid against an empty loading dataset.
  if (boardsState.sections is com.yanga.client.ui.LoadableUiState.Loading) {
    androidx.compose.foundation.layout.Box(modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
      androidx.compose.material3.CircularProgressIndicator()
    }
    return
  }
  BoardsScreen(
    state = boardsState,
    onBoardClick = { board ->
      boardsViewModel.onBoardOpened()
      onBoardClick(board)
    },
    onSearchClick = onSearchClick,
    modifier = modifier.fillMaxSize(),
  )
}
