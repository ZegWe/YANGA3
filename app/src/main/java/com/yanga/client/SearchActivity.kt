package com.yanga.client

import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.yanga.client.ui.BoardDestination
import com.yanga.client.ui.BoardPreview
import com.yanga.client.ui.SearchMode
import com.yanga.client.ui.SearchScreen
import com.yanga.client.ui.SearchViewModel
import com.yanga.client.ui.TopicNavigationTarget
import com.yanga.client.ui.TopicPreview
import com.yanga.client.ui.navigation.HomeActivityIntents
import com.yanga.client.ui.toData

class SearchActivity : YangaComposeActivity() {
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val mode = remember(intent) { searchMode(intent) }
    val viewModel = remember(app.repository, mode) { SearchViewModel(app.repository, mode) }
    val state by viewModel.state.collectAsState()
    val sessionData = loginSession?.toData()

    LaunchedEffect(sessionData) {
      viewModel.setSession(sessionData)
    }

    SearchScreen(
      state = state,
      onBack = { backDispatcher?.onBackPressed() },
      onQueryChange = viewModel::updateQuery,
      onSubmitSearch = { viewModel.submitSearch() },
      onScopeChange = viewModel::setScope,
      onSearchContentChange = viewModel::setSearchContent,
      onEssenceOnlyChange = viewModel::setEssenceOnly,
      onBoardClick = { board -> openBoard(context, board) },
      onTopicClick = { topic -> openTopic(context, topic) },
      modifier = Modifier,
    )
  }

  private fun openBoard(context: Context, board: BoardPreview) {
    context.startActivity(HomeActivityIntents.boardTopics(context, board))
  }

  private fun openTopic(context: Context, topic: TopicPreview) {
    when (val target = topic.navigationTarget) {
      is TopicNavigationTarget.Board -> context.startActivity(HomeActivityIntents.boardTopics(context, target.destination))
      TopicNavigationTarget.Thread -> context.startActivity(HomeActivityIntents.thread(context, topic))
    }
  }

  companion object {
    const val EXTRA_SCOPE = "search_scope"
    const val EXTRA_BOARD_ID = "search_board_id"
    const val EXTRA_BOARD_NAME = "search_board_name"
    const val EXTRA_BOARD_ICON_URL = "search_board_icon_url"
    const val EXTRA_BOARD_CATEGORY = "search_board_category"
    const val EXTRA_BOARD_IS_FAVORITE = "search_board_is_favorite"

    const val SCOPE_GLOBAL = "global"
    const val SCOPE_BOARD = "board"

    fun searchMode(intent: Intent): SearchMode =
      if (intent.getStringExtra(EXTRA_SCOPE) == SCOPE_BOARD) {
        SearchMode.BoardScoped(
          BoardDestination(
            id = intent.getStringExtra(EXTRA_BOARD_ID).orEmpty(),
            name = intent.getStringExtra(EXTRA_BOARD_NAME).orEmpty(),
            iconUrl = intent.getStringExtra(EXTRA_BOARD_ICON_URL),
            category = intent.getStringExtra(EXTRA_BOARD_CATEGORY).orEmpty(),
            isFavorite = intent.getBooleanExtra(EXTRA_BOARD_IS_FAVORITE, false),
          ),
        )
      } else {
        SearchMode.Global
      }
  }
}
