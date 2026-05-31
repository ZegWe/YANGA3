package com.yanga.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanga.client.ui.components.TopicListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val BOARD_GRID_COLUMNS = 3

/** Matches [androidx.compose.material3.TopAppBar] content row height without status-bar insets. */
private val BoardsScreenHeaderHeight = 64.dp

@Composable
private fun BoardsScreenHeader(
  onSearchClick: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(BoardsScreenHeaderHeight),
  ) {
    Text(
      text = "YANGA",
      modifier = Modifier.align(Alignment.Center),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.SemiBold,
    )
    IconButton(
      modifier = Modifier.align(Alignment.CenterEnd),
      onClick = onSearchClick,
    ) {
      Icon(
        imageVector = Icons.Outlined.Search,
        contentDescription = "搜索",
      )
    }
  }
}

private sealed interface BoardCategoryItem {
  val key: String
}

private data class BoardCategoryHeader(val title: String) : BoardCategoryItem {
  override val key: String = "header:$title"
}

private data class BoardCategoryBoard(val board: BoardPreview) : BoardCategoryItem {
  override val key: String = "board:${board.id}"
}

private data class BoardCategoryMessage(val text: String) : BoardCategoryItem {
  override val key: String = "message:$text"
}

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
  val categoryLabels = remember(sections) { listOf("收藏") + sections.map { it.name } }
  val categoryLabelKey = remember(categoryLabels) { categoryLabels.joinToString("\u0000") }
  var selectedCategoryIndex by rememberSaveable(categoryLabelKey) { androidx.compose.runtime.mutableIntStateOf(0) }
  selectedCategoryIndex = selectedCategoryIndex.coerceIn(0, categoryLabels.lastIndex.coerceAtLeast(0))
  val pagerState =
    rememberPagerState(
      initialPage = selectedCategoryIndex,
      pageCount = { categoryLabels.size.coerceAtLeast(1) },
    )
  val scope = rememberCoroutineScope()
  val selectedTabIndex by remember { derivedStateOf { pagerState.currentPage } }
  LaunchedEffect(selectedTabIndex) {
    selectedCategoryIndex = selectedTabIndex
  }
  LaunchedEffect(categoryLabels.size) {
    if (pagerState.currentPage > categoryLabels.lastIndex) {
      scope.launch { pagerState.scrollToPage(categoryLabels.lastIndex.coerceAtLeast(0)) }
    }
  }

  Column(
    modifier = modifier.fillMaxSize(),
  ) {
    BoardsScreenHeader()

    SecondaryScrollableTabRow(
      selectedTabIndex = selectedTabIndex,
      modifier = Modifier.fillMaxWidth(),
      edgePadding = 0.dp,
      divider = { HorizontalDivider() },
    ) {
      categoryLabels.forEachIndexed { index, label ->
        Tab(
          selected = selectedTabIndex == index,
          onClick = {
            selectedCategoryIndex = index
            navigateToCategoryTab(scope, pagerState, index)
          },
          text = { Text(text = label) },
        )
      }
    }

    HorizontalPager(
      state = pagerState,
      beyondViewportPageCount = 0,
      key = { page -> categoryLabels.getOrElse(page) { page.toString() } },
      modifier = Modifier.fillMaxSize(),
    ) { page ->
      BoardCategoryPage(
        page = page,
        subscribedBoards = state.subscribedBoards,
        sections = sections,
        onBoardClick = onBoardClick,
      )
    }
  }
}

@Composable
private fun BoardCategoryPage(
  page: Int,
  subscribedBoards: LoadableUiState<List<BoardPreview>>,
  sections: List<BoardSectionPreview>,
  onBoardClick: (BoardPreview) -> Unit,
) {
  val items =
    remember(page, subscribedBoards, sections) {
      buildBoardCategoryItems(
        page = page,
        subscribedBoards = subscribedBoards,
        sections = sections,
      )
    }
  val gridState = rememberSaveable(page, saver = LazyGridState.Saver) { LazyGridState() }
  BoardCategoryLazyGrid(
    items = items,
    onBoardClick = onBoardClick,
    state = gridState,
    modifier = Modifier.fillMaxSize(),
  )
}

@Composable
private fun BoardCategoryLazyGrid(
  items: List<BoardCategoryItem>,
  onBoardClick: (BoardPreview) -> Unit,
  state: LazyGridState,
  modifier: Modifier = Modifier,
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(BOARD_GRID_COLUMNS),
    state = state,
    modifier = modifier,
    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(
      items = items,
      key = { item -> item.key },
      span = { item ->
        when (item) {
          is BoardCategoryBoard -> GridItemSpan(1)
          is BoardCategoryHeader, is BoardCategoryMessage -> GridItemSpan(maxLineSpan)
        }
      },
    ) { item ->
      when (item) {
        is BoardCategoryHeader ->
          SectionHeader(
            title = item.title,
            modifier = Modifier.padding(bottom = 6.dp),
          )
        is BoardCategoryMessage ->
          TonalCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = item.text, style = MaterialTheme.typography.bodyMedium)
          }
        is BoardCategoryBoard ->
          FavoriteBoardCard(
            board = item.board,
            onClick = onBoardClick,
            modifier = Modifier.fillMaxWidth(),
          )
      }
    }
  }
}

private fun buildBoardCategoryItems(
  page: Int,
  subscribedBoards: LoadableUiState<List<BoardPreview>>,
  sections: List<BoardSectionPreview>,
): List<BoardCategoryItem> =
  buildList {
    if (page == 0) {
      addFavoriteBoardItems(
        state = subscribedBoards,
        emptyText = "No favorite boards",
        loadingText = "Loading favorite boards",
        loginRequiredText = "Sign in to load favorite boards",
      )
      return@buildList
    }

    val sectionGroups = sections.getOrNull(page - 1)?.groups.orEmpty()
    if (sectionGroups.isEmpty()) {
      add(BoardCategoryMessage("No boards available"))
      return@buildList
    }

    sectionGroups.forEach { group ->
      add(BoardCategoryHeader(group.name.ifBlank { "未命名分组" }))
      if (group.boards.isEmpty()) {
        add(BoardCategoryMessage("No boards available"))
      } else {
        group.boards.forEach { board -> add(BoardCategoryBoard(board)) }
      }
    }
  }

private fun MutableList<BoardCategoryItem>.addFavoriteBoardItems(
  state: LoadableUiState<List<BoardPreview>>,
  emptyText: String,
  loadingText: String,
  loginRequiredText: String,
) {
  when (state) {
    is LoadableUiState.Content -> {
      if (state.value.isEmpty()) {
        add(BoardCategoryMessage(emptyText))
      } else {
        state.value.forEach { board -> add(BoardCategoryBoard(board)) }
      }
    }
    is LoadableUiState.Empty -> add(BoardCategoryMessage(state.message))
    is LoadableUiState.Error -> add(BoardCategoryMessage(state.message))
    LoadableUiState.Loading -> add(BoardCategoryMessage(loadingText))
    LoadableUiState.LoginRequired -> add(BoardCategoryMessage(loginRequiredText))
  }
}

private fun navigateToCategoryTab(
  scope: CoroutineScope,
  pagerState: PagerState,
  toIndex: Int,
) {
  scope.launch {
    pagerState.animateScrollToPage(toIndex)
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
          state.value.forEachIndexed { index, topic ->
            TopicListItem(
              topic = topic,
              onClick = { onTopicClick(topic) },
            )
            if (index < state.value.lastIndex) {
              HorizontalDivider()
            }
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
  onClick: (BoardPreview) -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = { onClick(board) },
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
      Marker(text = board.marker, iconUrl = board.iconUrl, boardId = board.id)
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
