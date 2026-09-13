package com.yanga.client.ui.navigation

import androidx.compose.runtime.getValue

import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanga.client.ui.MainDestinationKey
import com.yanga.client.ui.LoginSessionUiState
import com.yanga.client.data.NgaReadOnlyRepository
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.yanga.client.ui.BoardDestination
import com.yanga.client.ui.SearchMode
import com.yanga.client.ui.SearchScreen
import com.yanga.client.ui.SearchViewModel
import com.yanga.client.ui.navigation.toNavigationKey
import com.yanga.client.ui.toData

@Composable
fun SearchRoute(
  board: BoardDestination?,
  repository: NgaReadOnlyRepository,
  loginSession: LoginSessionUiState?,
  onBack: () -> Unit,
  navigate: (MainDestinationKey) -> Unit,
) {
  val mode = remember(board) { board?.let(SearchMode::BoardScoped) ?: SearchMode.Global }
  val viewModel = viewModel<SearchViewModel> { SearchViewModel(repository, mode) }
  val state by viewModel.state.collectAsState()
  val sessionData = loginSession?.toData()

  LaunchedEffect(sessionData) {
    viewModel.setSession(sessionData)
  }

  SearchScreen(
    state = state,
    onBack = onBack,
    onQueryChange = viewModel::updateQuery,
    onSubmitSearch = { viewModel.submitSearch() },
    onScopeChange = viewModel::setScope,
    onSearchContentChange = viewModel::setSearchContent,
    onEssenceOnlyChange = viewModel::setEssenceOnly,
    onBoardClick = { board -> navigate(board.toNavigationKey()) },
    onTopicClick = { topic -> navigate(topic.toNavigationKey()) },
    modifier = Modifier,
  )
}
