package com.yanga.client.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.yanga.client.data.DefaultNgaReadOnlyRepository
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.theme.YangaTheme
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay

@Composable
fun MainScreen(
  loginSession: LoginSessionUiState? = null,
  repository: NgaReadOnlyRepository = remember { DefaultNgaReadOnlyRepository() },
  onLoginComplete: (LoginSessionUiState) -> Unit = {},
  onLogout: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val boardsViewModel = remember(repository) { BoardsListViewModel(repository) }
  val boardContentViewModel = remember(repository) { BoardContentViewModel(repository) }
  val threadContentViewModel = remember(repository) { ThreadContentViewModel(repository) }
  val messagesViewModel = remember(repository) { MessagesViewModel(repository) }
  val profileViewModel = remember(repository) { ProfileViewModel(repository) }
  val boardsState by boardsViewModel.state.collectAsState()
  val boardContentState by boardContentViewModel.state.collectAsState()
  val threadContentState by threadContentViewModel.state.collectAsState()
  val messagesState by messagesViewModel.state.collectAsState()
  val profileState by profileViewModel.state.collectAsState()
  val backStack = rememberNavBackStack(MainDestinationKey.Home)
  val context = LocalContext.current
  val sessionData = loginSession?.toData()

  LaunchedEffect(loginSession, profileState.forumEndpoint, repository) {
    if (repository is DefaultNgaReadOnlyRepository) {
      repository.setBaseUrl(profileState.forumEndpoint)
    }
    boardsViewModel.refresh(sessionData)
    messagesViewModel.refresh(sessionData)
    profileViewModel.refresh(sessionData)
  }

  LaunchedEffect(boardsState) {
    preloadBoardIcons(
      context = context,
      boardsState = boardsState,
    )
  }

  LaunchedEffect(boardContentState) {
    val board = boardContentState ?: return@LaunchedEffect
    val boardKey = MainDestinationKey.Board(board.fid)
    if (backStack.lastOrNull() != boardKey) {
      backStack.add(boardKey)
    }
  }

  LaunchedEffect(threadContentState) {
    val thread = threadContentState ?: return@LaunchedEffect
    val threadId = thread.title.ifBlank { "thread" }
    val threadKey = MainDestinationKey.Thread(threadId)
    if (backStack.lastOrNull() != threadKey) {
      backStack.add(threadKey)
    }
  }

  NavDisplay(
    backStack = backStack,
    onBack = {
      when (backStack.lastOrNull()) {
        is MainDestinationKey.Thread -> {
          threadContentViewModel.backFromThread()
          popBackStack(backStack)
        }
        is MainDestinationKey.Board -> {
          boardContentViewModel.backFromBoard()
          popBackStack(backStack)
        }
        is MainDestinationKey.Login -> popBackStack(backStack)
        else -> {
          if (backStack.count() > 1) {
            popBackStack(backStack)
          }
        }
      }
    },
    entryProvider =
      entryProvider {
        entry<MainDestinationKey.Home> {
          MainRootScaffold(
            selectedTab = MainTab.Home,
            loginSession = loginSession,
            boardsState = boardsState,
            messagesState = messagesState,
            profileState = profileState,
            onTabSelected = { selectTopLevelDestination(backStack, it) },
            onLoginClick = { backStack.add(MainDestinationKey.Login) },
            onLogout = onLogout,
            onEndpointChange = { profileViewModel.setEndpoint(it) },
            onBoardClick = { boardContentViewModel.openBoard(sessionData, it) },
            onTopicClick = { threadContentViewModel.openThread(sessionData, it) },
            modifier = modifier,
          )
        }

        entry<MainDestinationKey.Messages> {
          MainRootScaffold(
            selectedTab = MainTab.Messages,
            loginSession = loginSession,
            boardsState = boardsState,
            messagesState = messagesState,
            profileState = profileState,
            onTabSelected = { selectTopLevelDestination(backStack, it) },
            onLoginClick = { backStack.add(MainDestinationKey.Login) },
            onLogout = onLogout,
            onEndpointChange = { profileViewModel.setEndpoint(it) },
            onBoardClick = { boardContentViewModel.openBoard(sessionData, it) },
            onTopicClick = { threadContentViewModel.openThread(sessionData, it) },
            modifier = modifier,
          )
        }

        entry<MainDestinationKey.Profile> {
          MainRootScaffold(
            selectedTab = MainTab.Profile,
            loginSession = loginSession,
            boardsState = boardsState,
            messagesState = messagesState,
            profileState = profileState,
            onTabSelected = { selectTopLevelDestination(backStack, it) },
            onLoginClick = { backStack.add(MainDestinationKey.Login) },
            onLogout = onLogout,
            onEndpointChange = { profileViewModel.setEndpoint(it) },
            onBoardClick = { boardContentViewModel.openBoard(sessionData, it) },
            onTopicClick = { threadContentViewModel.openThread(sessionData, it) },
            modifier = modifier,
          )
        }

        entry<MainDestinationKey.Login> {
          PasswordLoginScreen(
            onLoginComplete = {
              popBackStack(backStack)
              onLoginComplete(it)
            },
            onClose = {
              popBackStack(backStack)
            },
            modifier = modifier,
          )
        }

        entry<MainDestinationKey.Board> {
          val boardState = boardContentState
          if (boardState != null) {
            BoardTopicListScreen(
              state = boardState,
              onBack = {
                boardContentViewModel.backFromBoard()
                popBackStack(backStack)
              },
              onTopicClick = { threadContentViewModel.openThread(sessionData, it) },
              onToggleFavorite = {
                val active = boardContentState ?: return@BoardTopicListScreen
                val board = BoardPreview(
                  id = active.fid,
                  name = active.boardName,
                  metadata = "fid: ${active.fid}",
                  marker = active.boardName.take(1),
                  iconUrl = active.iconUrl,
                  category = active.category,
                  isFavorite = active.isFavorite,
                )
                boardsViewModel.toggleBoardFavorite(board)
                boardContentViewModel.setFavorite(!active.isFavorite)
              },
              modifier = modifier,
            )
          } else {
            Box(modifier = Modifier.fillMaxSize())
          }
        }

        entry<MainDestinationKey.Thread> {
          val threadState = threadContentState
          if (threadState != null) {
            ThreadReadingScreen(
              state = threadState,
              onBack = {
                threadContentViewModel.backFromThread()
                popBackStack(backStack)
              },
              modifier = modifier,
            )
          } else {
            Box(modifier = Modifier.fillMaxSize())
          }
        }
      },
  )
}

private fun popBackStack(backStack: NavBackStack<NavKey>) {
  if (backStack.count() > 1) {
    backStack.removeAt(backStack.lastIndex)
  }
}

private fun selectTopLevelDestination(
  backStack: NavBackStack<NavKey>,
  tab: MainTab,
) {
  val destination =
    when (tab) {
      MainTab.Home -> MainDestinationKey.Home
      MainTab.Messages -> MainDestinationKey.Messages
      MainTab.Profile -> MainDestinationKey.Profile
    }

  if (backStack.lastOrNull() == destination) return

  while (backStack.count() > 1) {
    backStack.removeAt(backStack.lastIndex)
  }
  backStack[0] = destination
}

@Composable
private fun MainRootScaffold(
  selectedTab: MainTab,
  loginSession: LoginSessionUiState?,
  boardsState: BoardsUiState,
  messagesState: MessagesUiState,
  profileState: ProfileUiState,
  onTabSelected: (MainTab) -> Unit,
  onLoginClick: () -> Unit,
  onLogout: () -> Unit,
  onEndpointChange: (String) -> Unit,
  onBoardClick: (BoardPreview) -> Unit,
  onTopicClick: (TopicPreview) -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    bottomBar = {
      YangaBottomNavigation(selectedTab = selectedTab, onTabSelected = onTabSelected)
    },
  ) { paddingValues ->
    MainTabContent(
      selectedTab = selectedTab,
      loginSession = loginSession,
      boardsState = boardsState,
      messagesState = messagesState,
      profileState = profileState,
      onLoginClick = onLoginClick,
      onLogout = onLogout,
      onEndpointChange = onEndpointChange,
      onBoardClick = onBoardClick,
      onTopicClick = onTopicClick,
      paddingValues = paddingValues,
    )
  }
}

@Composable
private fun MainTabContent(
  selectedTab: MainTab,
  loginSession: LoginSessionUiState?,
  boardsState: BoardsUiState,
  messagesState: MessagesUiState,
  profileState: ProfileUiState,
  onLoginClick: () -> Unit,
  onLogout: () -> Unit,
  onEndpointChange: (String) -> Unit,
  onBoardClick: (BoardPreview) -> Unit,
  onTopicClick: (TopicPreview) -> Unit,
  paddingValues: PaddingValues,
) {
  val defaultContentModifier =
    Modifier
      .fillMaxSize()
      .safeDrawingPadding()
      .padding(paddingValues)
      .padding(horizontal = 20.dp, vertical = 16.dp)
  val homeContentModifier =
    Modifier
      .fillMaxSize()
      .padding(paddingValues)

  when (selectedTab) {
    MainTab.Home ->
      BoardsScreen(
        state = boardsState,
        onBoardClick = onBoardClick,
        modifier = homeContentModifier,
      )
    MainTab.Messages ->
      MessagesScreen(
        loginSession = loginSession,
        state = messagesState,
        onLoginClick = onLoginClick,
        modifier = defaultContentModifier,
      )

    MainTab.Profile ->
      ProfileScreen(
        loginSession = loginSession,
        state = profileState,
        onLoginClick = onLoginClick,
        onLogout = onLogout,
        onEndpointChange = onEndpointChange,
        modifier = defaultContentModifier,
      )
  }
}

private fun preloadBoardIcons(context: android.content.Context, boardsState: BoardsUiState) {
  val iconUrls =
    buildList {
      ((boardsState.subscribedBoards as? LoadableUiState.Content)?.value ?: emptyList())
        .mapNotNullTo(this) { it.iconUrl?.takeIf(String::isNotBlank) }

      ((boardsState.sections as? LoadableUiState.Content)?.value ?: emptyList())
        .flatMap { it.groups }
        .flatMap { it.boards }
        .mapNotNullTo(this) { it.iconUrl?.takeIf(String::isNotBlank) }
    }
      .distinct()
      .take(120)

  if (iconUrls.isEmpty()) return

  val imageLoader = context.imageLoader
  iconUrls.forEach { url ->
    imageLoader.enqueue(
      ImageRequest.Builder(context)
        .data(url)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
        .build(),
    )
  }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  YangaTheme { MainScreen() }
}

@Preview(showBackground = true, widthDp = 340)
@Composable
fun MainScreenPortraitPreview() {
  YangaTheme {
    MainScreen(
      loginSession = LoginSessionUiState(
        username = "测试用户",
        uid = "42",
        cookie = "ngaPassportUid=42; ngaPassportCid=abc",
      ),
    )
  }
}








