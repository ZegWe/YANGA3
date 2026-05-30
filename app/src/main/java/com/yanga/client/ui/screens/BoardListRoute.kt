package com.yanga.client.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.yanga.client.YangaApplication
import com.yanga.client.ui.BoardsListViewModel
import com.yanga.client.ui.BoardsScreen
import com.yanga.client.ui.LoginSessionUiState
import com.yanga.client.ui.ProfileViewModel
import com.yanga.client.ui.navigation.HomeActivityIntents
import com.yanga.client.ui.toData

@Composable
fun BoardListRoute(
  loginSession: LoginSessionUiState?,
  app: YangaApplication,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val boardsViewModel =
    remember(app.repository, app.boardsCatalog) {
      BoardsListViewModel(app.repository, app.boardsCatalog)
    }
  val profileViewModel = remember(app.repository) { ProfileViewModel(app.repository) }
  val boardsState by boardsViewModel.state.collectAsState()
  val profileState by profileViewModel.state.collectAsState()
  val sessionData = loginSession?.toData()

  LaunchedEffect(loginSession, profileState.forumEndpoint, app.repository) {
    app.repository.setBaseUrl(profileState.forumEndpoint)
    boardsViewModel.applyEndpoint(profileState.forumEndpoint)
    boardsViewModel.refresh(sessionData)
    profileViewModel.refresh(sessionData)
  }

  BoardsScreen(
    state = boardsState,
    onBoardClick = { board ->
      boardsViewModel.onBoardOpened()
      context.startActivity(HomeActivityIntents.boardTopics(context, board))
    },
    modifier = modifier.fillMaxSize(),
  )
}
