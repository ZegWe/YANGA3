package com.yanga.client.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yanga.client.YangaApplication
import com.yanga.client.data.DefaultNgaReadOnlyRepository
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.data.boards.BoardsCatalog
import com.yanga.client.theme.YangaTheme
import com.yanga.client.ui.navigation.HomeActivityIntents
import com.yanga.client.ui.screens.BoardListRoute
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay

@Composable
fun MainScreen(
  loginSession: LoginSessionUiState? = null,
  repository: NgaReadOnlyRepository = remember { DefaultNgaReadOnlyRepository() },
  boardsCatalog: BoardsCatalog? = null,
  app: YangaApplication? = null,
  onLogout: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val messagesViewModel = remember(repository) { MessagesViewModel(repository) }
  val profileViewModel = remember(repository) { ProfileViewModel(repository) }
  val messagesState by messagesViewModel.state.collectAsState()
  val profileState by profileViewModel.state.collectAsState()
  val backStack = rememberNavBackStack(MainDestinationKey.Home)
  val sessionData = loginSession?.toData()
  val onLoginClick = remember(context) { { context.startActivity(HomeActivityIntents.login(context)) } }

  LaunchedEffect(loginSession, profileState.forumEndpoint, repository) {
    if (repository is DefaultNgaReadOnlyRepository) {
      repository.setBaseUrl(profileState.forumEndpoint)
    }
    messagesViewModel.refresh(sessionData)
    profileViewModel.refresh(sessionData)
  }

  NavDisplay(
    backStack = backStack,
    onBack = {
      if (backStack.count() > 1) {
        popBackStack(backStack)
      }
    },
    entryProvider =
      entryProvider {
        entry<MainDestinationKey.Home> {
          MainRootScaffold(
            selectedTab = MainTab.Home,
            loginSession = loginSession,
            messagesState = messagesState,
            profileState = profileState,
            onTabSelected = { selectTopLevelDestination(backStack, it) },
            onLoginClick = onLoginClick,
            onLogout = onLogout,
            onEndpointChange = { profileViewModel.setEndpoint(it) },
            homeContent = {
              if (app != null) {
                BoardListRoute(
                  loginSession = loginSession,
                  app = app,
                )
              }
            },
            modifier = modifier,
          )
        }

        entry<MainDestinationKey.Messages> {
          MainRootScaffold(
            selectedTab = MainTab.Messages,
            loginSession = loginSession,
            messagesState = messagesState,
            profileState = profileState,
            onTabSelected = { selectTopLevelDestination(backStack, it) },
            onLoginClick = onLoginClick,
            onLogout = onLogout,
            onEndpointChange = { profileViewModel.setEndpoint(it) },
            modifier = modifier,
          )
        }

        entry<MainDestinationKey.Profile> {
          MainRootScaffold(
            selectedTab = MainTab.Profile,
            loginSession = loginSession,
            messagesState = messagesState,
            profileState = profileState,
            onTabSelected = { selectTopLevelDestination(backStack, it) },
            onLoginClick = onLoginClick,
            onLogout = onLogout,
            onEndpointChange = { profileViewModel.setEndpoint(it) },
            modifier = modifier,
          )
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
  messagesState: MessagesUiState,
  profileState: ProfileUiState,
  onTabSelected: (MainTab) -> Unit,
  onLoginClick: () -> Unit,
  onLogout: () -> Unit,
  onEndpointChange: (String) -> Unit,
  homeContent: @Composable () -> Unit = {},
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
      messagesState = messagesState,
      profileState = profileState,
      onLoginClick = onLoginClick,
      onLogout = onLogout,
      onEndpointChange = onEndpointChange,
      homeContent = homeContent,
      paddingValues = paddingValues,
    )
  }
}

@Composable
private fun MainTabContent(
  selectedTab: MainTab,
  loginSession: LoginSessionUiState?,
  messagesState: MessagesUiState,
  profileState: ProfileUiState,
  onLoginClick: () -> Unit,
  onLogout: () -> Unit,
  onEndpointChange: (String) -> Unit,
  homeContent: @Composable () -> Unit,
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
      Box(modifier = homeContentModifier) {
        homeContent()
      }
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








