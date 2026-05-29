package com.yanga.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
internal fun HomeScreen(
  loginSession: LoginSessionUiState?,
  modifier: Modifier = Modifier,
  state: HomeUiState = HomeUiState(),
  onLoginClick: () -> Unit,
  onBoardClick: (BoardPreview) -> Unit = {},
  onTopicClick: (TopicPreview) -> Unit = {},
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    PageHeader(
      title = "Yanga",
      subtitle = if (loginSession == null) {
        "Browse NGA boards before signing in"
      } else {
        "Signed in as ${loginSession.username} - UID ${loginSession.uid}"
      },
    )

    SearchPill(text = "Search boards, topics, and users")

    if (loginSession == null) {
      LoginPrompt(onLoginClick = onLoginClick)
    }

    SectionHeader(title = "Active discussions", trailing = "Latest")
    TopicListState(state = state.activeTopics, onTopicClick = onTopicClick)
  }
}

@Composable
internal fun BoardsScreen(
  modifier: Modifier = Modifier,
  state: BoardsUiState = BoardsUiState(),
  onBoardClick: (BoardPreview) -> Unit = {},
) {
  val sections = (state.sections as? LoadableUiState.Content)?.value.orEmpty()
  val categoryLabels = listOf("收藏") + sections.map { it.name }
  var selectedCategoryIndex by rememberSaveable(categoryLabels) { androidx.compose.runtime.mutableIntStateOf(0) }
  selectedCategoryIndex = selectedCategoryIndex.coerceIn(0, categoryLabels.lastIndex.coerceAtLeast(0))
  val pagerState = rememberPagerState(initialPage = selectedCategoryIndex, pageCount = { categoryLabels.size.coerceAtLeast(1) })
  val scope = rememberCoroutineScope()
  LaunchedEffect(pagerState.currentPage) {
    selectedCategoryIndex = pagerState.currentPage
  }

  Column(
    modifier = modifier.fillMaxSize(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 0.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "主页",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
      )
      Spacer(modifier = Modifier.weight(1f))
      IconButton(
        onClick = {},
      ) {
        Icon(
          imageVector = Icons.Outlined.Search,
          contentDescription = "搜索",
        )
      }
    }

    PrimaryScrollableTabRow(
      selectedTabIndex = pagerState.currentPage,
      modifier = Modifier.fillMaxWidth(),
      edgePadding = 0.dp,
      divider = {
        HorizontalDivider(
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
          thickness = 1.dp,
        )
      },
      indicator = {
        TabRowDefaults.PrimaryIndicator(
          modifier = Modifier.tabIndicatorOffset(
            selectedTabIndex = pagerState.currentPage,
            matchContentSize = false,
          ),
          color = MaterialTheme.colorScheme.primary,
        )
      },
    ) {
      categoryLabels.forEachIndexed { index, label ->
        Tab(
          selected = pagerState.currentPage == index,
          onClick = {
            selectedCategoryIndex = index
            scope.launch { pagerState.animateScrollToPage(index) }
          },
          text = { Text(text = label) },
        )
      }
    }

    HorizontalPager(
      state = pagerState,
      beyondViewportPageCount = 1,
      modifier = Modifier
        .fillMaxSize(),
    ) { page ->
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        if (page == 0) {
          item {
            BoardGridState(
              state = state.subscribedBoards,
              emptyText = "No favorite boards",
              loadingText = "Loading favorite boards",
              loginRequiredText = "Sign in to load favorite boards",
              columns = 3,
              onBoardClick = onBoardClick,
            )
          }
        } else {
          val sectionGroups = sections[page - 1].groups
          if (sectionGroups.isEmpty()) {
            item { StateMessage(text = "No boards available") }
          } else {
            items(sectionGroups.size) { index ->
              val group = sectionGroups[index]
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(title = group.name.ifBlank { "未命名分组" })
                if (group.boards.isEmpty()) {
                  StateMessage(text = "No boards available")
                } else {
                  BoardGrid(
                    boards = group.boards,
                    columns = 3,
                    onBoardClick = onBoardClick,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LoginPrompt(onLoginClick: () -> Unit, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    shape = MaterialTheme.shapes.large,
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "Sign in to sync Favorites and History",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = "Public browsing stays available. Sign in when you want to post, manage favorites, or restore subscribed boards.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      Button(onClick = onLoginClick) {
        Text(text = "Sign in")
      }
    }
  }
}

@Composable
private fun BoardGridState(
  state: LoadableUiState<List<BoardPreview>>,
  onBoardClick: (BoardPreview) -> Unit,
  modifier: Modifier = Modifier,
  emptyText: String = "No favorite boards",
  loadingText: String = "Loading favorite boards",
  loginRequiredText: String = "Sign in to load favorite boards",
  columns: Int = 2,
) {
  when (state) {
    is LoadableUiState.Content -> {
      if (state.value.isEmpty()) {
        StateMessage(text = emptyText)
      } else {
        BoardGrid(
          boards = state.value,
          onBoardClick = onBoardClick,
          modifier = modifier,
          columns = columns,
        )
      }
    }
    is LoadableUiState.Empty -> StateMessage(text = state.message)
    is LoadableUiState.Error -> StateMessage(text = state.message)
    LoadableUiState.Loading -> StateMessage(text = loadingText)
    LoadableUiState.LoginRequired -> StateMessage(text = loginRequiredText)
  }
}

@Composable
private fun BoardGrid(
  boards: List<BoardPreview>,
  onBoardClick: (BoardPreview) -> Unit,
  modifier: Modifier = Modifier,
  columns: Int = 2,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    boards.chunked(columns).forEach { rowBoards ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        rowBoards.forEach { board ->
          FavoriteBoardCard(
            board = board,
            onClick = { onBoardClick(board) },
            modifier = Modifier.weight(1f),
          )
        }
        repeat(columns - rowBoards.size) {
          Column(modifier = Modifier.weight(1f)) {}
        }
      }
    }
  }
}

@Composable
private fun TopicListState(
  state: LoadableUiState<List<TopicPreview>>,
  onTopicClick: (TopicPreview) -> Unit,
  modifier: Modifier = Modifier,
) {
  TonalCard(modifier = modifier) {
    when (state) {
      is LoadableUiState.Content -> {
        if (state.value.isEmpty()) {
          Text(text = "No active discussions", style = MaterialTheme.typography.bodyMedium)
        } else {
          state.value.forEach { topic ->
            TopicRow(topic = topic, onClick = { onTopicClick(topic) })
          }
        }
      }
      is LoadableUiState.Empty -> Text(text = state.message, style = MaterialTheme.typography.bodyMedium)
      is LoadableUiState.Error ->
        Text(text = state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
      LoadableUiState.Loading -> Text(text = "Loading active discussions", style = MaterialTheme.typography.bodyMedium)
      LoadableUiState.LoginRequired -> Text(text = "Sign in to load active discussions", style = MaterialTheme.typography.bodyMedium)
    }
  }
}

@Composable
private fun BoardListState(
  state: LoadableUiState<List<BoardPreview>>,
  loginRequiredText: String,
  onBoardClick: (BoardPreview) -> Unit,
  modifier: Modifier = Modifier,
) {
  TonalCard(modifier = modifier) {
    when (state) {
      is LoadableUiState.Content -> {
        if (state.value.isEmpty()) {
          Text(text = "No boards available", style = MaterialTheme.typography.bodyMedium)
        } else {
          state.value.forEach { board ->
            BoardListRow(board = board, onClick = { onBoardClick(board) })
          }
        }
      }
      is LoadableUiState.Empty -> Text(text = state.message, style = MaterialTheme.typography.bodyMedium)
      is LoadableUiState.Error ->
        Text(text = state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
      LoadableUiState.Loading -> Text(text = "Loading boards", style = MaterialTheme.typography.bodyMedium)
      LoadableUiState.LoginRequired -> Text(text = loginRequiredText, style = MaterialTheme.typography.bodyMedium)
    }
  }
}

@Composable
private fun StateMessage(text: String, modifier: Modifier = Modifier) {
  TonalCard(modifier = modifier) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun FavoriteBoardCard(
  board: BoardPreview,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    shape = MaterialTheme.shapes.large,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Marker(text = board.marker, iconUrl = board.iconUrl)
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = board.name,
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      board.badge?.let {
        Text(
          text = "$it new",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}








