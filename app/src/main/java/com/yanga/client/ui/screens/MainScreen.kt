package com.yanga.client.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yanga.client.YangaApplication
import com.yanga.client.data.DefaultNgaReadOnlyRepository
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.data.boards.BoardsCatalog
import com.yanga.client.theme.ThemePreferences
import com.yanga.client.theme.YangaTheme
import com.yanga.client.ui.navigation.BoardTopicListRoute
import com.yanga.client.ui.navigation.SearchRoute
import com.yanga.client.ui.navigation.ThreadRoute
import com.yanga.client.ui.navigation.WebViewRoute
import com.yanga.client.ui.navigation.toNavigationKey
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
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
  onLoginComplete: (LoginSessionUiState) -> Unit = {},
  pendingDestination: MainDestinationKey? = null,
  onDestinationConsumed: () -> Unit = {},
  onLogout: () -> Unit = {},
  themePreferences: ThemePreferences = ThemePreferences(),
  onThemePreferencesChange: (ThemePreferences) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val messagesViewModel = viewModel<MessagesViewModel> { MessagesViewModel(repository) }
  val profileViewModel = viewModel<ProfileViewModel> { ProfileViewModel(repository) }
  val messagesState by messagesViewModel.state.collectAsState()
  val profileState by profileViewModel.state.collectAsState()
  val backStack = rememberNavBackStack(MainDestinationKey.Home)
  val sessionData = loginSession?.toData()
  val navigate: (MainDestinationKey) -> Unit = { backStack.add(it) }
  val onBack: () -> Unit = { popBackStack(backStack) }
  val onLoginClick: () -> Unit = { navigate(MainDestinationKey.Login) }
  LaunchedEffect(pendingDestination) {
    pendingDestination?.let {
      navigate(it)
      onDestinationConsumed()
    }
  }

  LaunchedEffect(loginSession, profileState.forumEndpoint, repository) {
    if (repository is DefaultNgaReadOnlyRepository) {
      repository.setBaseUrl(profileState.forumEndpoint)
    }
    messagesViewModel.refresh(sessionData)
    profileViewModel.refresh(sessionData)
  }

  NavDisplay(
    backStack = backStack,
    entryDecorators = listOf(
      rememberSaveableStateHolderNavEntryDecorator(),
      rememberViewModelStoreNavEntryDecorator(),
    ),
    transitionSpec = {
      if (initialState.previousEntries.isEmpty() && targetState.previousEntries.isEmpty()) {
        // Replacing a root tab does not change the navigation hierarchy.
        EnterTransition.None togetherWith ExitTransition.None
      } else {
        slideInHorizontally(
          animationSpec = tween(MainNavigationSlideSpec.DurationMillis),
          initialOffsetX = MainNavigationSlideSpec::forwardEnterOffset,
        ) togetherWith
          slideOutHorizontally(
            animationSpec = tween(MainNavigationSlideSpec.DurationMillis),
            targetOffsetX = MainNavigationSlideSpec::forwardExitOffset,
          )
      }
    },
    popTransitionSpec = {
      if (initialState.previousEntries.isEmpty() && targetState.previousEntries.isEmpty()) {
        EnterTransition.None togetherWith ExitTransition.None
      } else {
        slideInHorizontally(
          animationSpec = tween(MainNavigationSlideSpec.DurationMillis),
          initialOffsetX = MainNavigationSlideSpec::popEnterOffset,
        ) togetherWith
          slideOutHorizontally(
            animationSpec = tween(MainNavigationSlideSpec.DurationMillis),
            targetOffsetX = MainNavigationSlideSpec::popExitOffset,
          )
      }
    },
    predictivePopTransitionSpec = {
      // NavDisplay seeks this transition with the gesture and restores it on cancellation.
      slideInHorizontally(
        animationSpec = tween(MainNavigationSlideSpec.DurationMillis),
        initialOffsetX = MainNavigationSlideSpec::popEnterOffset,
      ) togetherWith
        slideOutHorizontally(
          animationSpec = tween(MainNavigationSlideSpec.DurationMillis),
          targetOffsetX = MainNavigationSlideSpec::popExitOffset,
        )
    },
    onBack = {
      if (backStack.count() > 1) {
        popBackStack(backStack)
      }
    },
    entryProvider =
      entryProvider {
        entry<MainDestinationKey.Home> {
          var selectedTab by rememberSaveable { mutableStateOf(MainTab.Home) }
          MainRootScaffold(
            selectedTab = selectedTab,
            loginSession = loginSession,
            messagesState = messagesState,
            profileState = profileState,
            onTabSelected = { selectedTab = it },
            onLoginClick = onLoginClick,
            onLogout = onLogout,
            onEndpointChange = { profileViewModel.setEndpoint(it) },
            onThemeSettingsClick = { navigate(MainDestinationKey.ThemeSettings) },
            onAboutClick = { navigate(MainDestinationKey.About) },
            homeContent = {
              BoardListRoute(
                loginSession = loginSession,
                repository = repository,
                boardsCatalog = boardsCatalog ?: app?.boardsCatalog,
                forumEndpoint = profileState.forumEndpoint,
                onBoardClick = { navigate(it.toNavigationKey()) },
                onSearchClick = { navigate(MainDestinationKey.Search()) },
              )
            },
            modifier = modifier,
          )
        }
        entry<MainDestinationKey.Board> { key ->
          BoardTopicListRoute(key.destination, repository, app, loginSession, onBack, navigate)
        }
        entry<MainDestinationKey.Thread> { key ->
          ThreadRoute(key.destination, repository, profileState.forumEndpoint, loginSession, onBack, navigate)
        }
        entry<MainDestinationKey.Search> { key ->
          SearchRoute(key.board, repository, loginSession, onBack, navigate)
        }
        entry<MainDestinationKey.Web> { key ->
          WebViewRoute(key.url, key.title, key.baseUrl, loginSession?.cookie.orEmpty(), onBack)
        }
        entry<MainDestinationKey.Login> {
          PasswordLoginScreen(
            onLoginComplete = { session ->
              onLoginComplete(session)
              onBack()
            },
            onClose = onBack,
            modifier = Modifier.fillMaxSize(),
          )
        }

        entry<MainDestinationKey.About> { AboutScreen(onBack = onBack) }
        entry<MainDestinationKey.ThemeSettings> {
          ThemeSettingsScreen(
            preferences = themePreferences,
            onPreferencesChange = onThemePreferencesChange,
            onBack = { popBackStack(backStack) },
            modifier = modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .safeDrawingPadding()
              .padding(horizontal = 20.dp, vertical = 16.dp),
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
  onThemeSettingsClick: () -> Unit,
  onAboutClick: () -> Unit,
  homeContent: @Composable () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val tabStateHolder = rememberSaveableStateHolder()
  Scaffold(
    modifier = modifier.fillMaxSize(),
    bottomBar = {
      YangaBottomNavigation(selectedTab = selectedTab, onTabSelected = onTabSelected)
    },
  ) { paddingValues ->
    tabStateHolder.SaveableStateProvider(selectedTab.name) {
      MainTabContent(
        selectedTab = selectedTab,
        loginSession = loginSession,
        messagesState = messagesState,
        profileState = profileState,
        onLoginClick = onLoginClick,
        onLogout = onLogout,
        onEndpointChange = onEndpointChange,
        onThemeSettingsClick = onThemeSettingsClick,
        onAboutClick = onAboutClick,
        homeContent = homeContent,
        paddingValues = paddingValues,
      )
    }
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
  onThemeSettingsClick: () -> Unit,
  onAboutClick: () -> Unit,
  homeContent: @Composable () -> Unit,
  paddingValues: PaddingValues,
) {
  val defaultContentModifier =
    Modifier
      .fillMaxSize()
      .safeDrawingPadding()
      .padding(paddingValues)
      .padding(horizontal = 20.dp, vertical = 16.dp)
  val profileContentModifier =
    Modifier
      .fillMaxSize()
      .safeDrawingPadding()
      .padding(paddingValues)
      .padding(vertical = 16.dp)
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
        onThemeSettingsClick = onThemeSettingsClick,
        onAboutClick = onAboutClick,
        modifier = profileContentModifier,
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
