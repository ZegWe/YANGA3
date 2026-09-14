package com.yanga.client.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollTo
import com.yanga.client.api.NgaBoardGroup
import com.yanga.client.api.NgaBoardSection
import com.yanga.client.api.NgaBoardSummary
import com.yanga.client.api.NgaMessageSummary
import com.yanga.client.api.NgaNotificationSummary
import com.yanga.client.api.NgaProfileCounters
import com.yanga.client.api.NgaThreadRead
import com.yanga.client.api.NgaTopicList
import com.yanga.client.api.NgaTopicSummary
import com.yanga.client.data.BoardsReadData
import com.yanga.client.data.HomeReadData
import com.yanga.client.data.LocalFavoriteBoard
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.MessagesReadData
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.data.ProfileReadData
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** UI tests for [MainScreen]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun userPostCountOpensThatUsersTopics() {
    var requestedUid = ""
    val repository = object : NgaReadOnlyRepository by fakeRepository() {
      override suspend fun loadUser(session: LoginSessionData?, uid: String) = Result.success(
        com.yanga.client.api.NgaUserProfile(uid, "其他用户", null, "", 20, null),
      )
      override suspend fun loadUserTopics(session: LoginSessionData?, uid: String, page: Int): Result<com.yanga.client.api.NgaPersonalTopicPage> {
        requestedUid = uid
        return Result.success(com.yanga.client.api.NgaPersonalTopicPage(listOf(com.yanga.client.api.NgaPersonalTopic("100", null, "其他用户的帖子", "")), false))
      }
    }
    composeTestRule.setContent { MainScreen(repository = repository, loginSession = LoginSessionUiState("自己", "42", "test"), pendingDestination = MainDestinationKey.User("99")) }
    waitUntilTextExists("其他用户")
    composeTestRule.onNodeWithContentDescription("查看用户发帖").performClick()
    waitUntilTextExists("其他用户的帖子")
    composeTestRule.runOnIdle { org.junit.Assert.assertEquals("99", requestedUid) }
    composeTestRule.onNodeWithText("其他用户的主题").assertExists()
  }

  @Test
  fun checkedInStateUsesCheckIconAndStillShowsFeedback() {
    composeTestRule.setContent { ProfileScreen(
      loginSession = LoginSessionUiState("user", "42", "test"),
      state = ProfileUiState(session = LoadableUiState.Content(LoginSessionData(username = "user", uid = "42", cookie = "test")), checkedIn = true, checkInMessage = "今天已经签到"),
      onLoginClick = {}, onLogout = {},
    ) }
    composeTestRule.onAllNodesWithContentDescription("未签到").assertCountEquals(0)
    composeTestRule.onNodeWithContentDescription("已签到").performClick()
    composeTestRule.onNodeWithText("今天已经签到").assertExists()
  }

  @Test
  fun notificationCardOpensIndependentPage() {
    val repository = object : NgaReadOnlyRepository by fakeRepository() {
      override suspend fun loadNotifications(session: LoginSessionData?) = Result.success(listOf(
        NgaNotificationSummary("1", "测试通知", "这是一条回复提醒", null, 1),
      ))
    }
    composeTestRule.setContent { MainScreen(repository = repository, loginSession = LoginSessionUiState("user", "42", "test")) }
    composeTestRule.onNodeWithContentDescription("Profile", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithText("通知").performClick()
    waitUntilTextExists("测试通知")
    composeTestRule.onNodeWithText("这是一条回复提醒").assertExists()
    composeTestRule.onNodeWithContentDescription("刷新通知").assertExists()
    composeTestRule.onAllNodesWithText("关闭").assertCountEquals(0)
    composeTestRule.onNodeWithContentDescription("返回").performClick()
    composeTestRule.onNodeWithText("账号设置").assertExists()
  }

  @Test
  fun profileCardsOpenDestinationsAndCheckInShowsDialog() {
    var opened = ""
    var userOpened = false
    var checks = 0
    composeTestRule.setContent {
      ProfileScreen(
        loginSession = LoginSessionUiState("user", "42", "test"),
        state = ProfileUiState(session = LoadableUiState.Content(LoginSessionData(username = "user", uid = "42", cookie = "test")), checkInMessage = "今天已经签到"),
        onLoginClick = {}, onLogout = {}, onUserClick = { userOpened = true },
        onPersonalTopics = { opened = it }, onCheckIn = { checks++ },
      )
    }
    composeTestRule.onAllNodesWithText("查看用户页面").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("我的主题").assertCountEquals(0)
    composeTestRule.onNodeWithContentDescription("查看用户资料").performClick()
    composeTestRule.runOnIdle { assertTrue(userOpened) }
    composeTestRule.onAllNodesWithText("主题")[0].performClick()
    composeTestRule.runOnIdle { org.junit.Assert.assertEquals("Topics", opened) }
    composeTestRule.onNodeWithText("回复").performClick()
    composeTestRule.runOnIdle { org.junit.Assert.assertEquals("Replies", opened) }
    composeTestRule.onAllNodesWithText("今天已经签到").assertCountEquals(0)
    composeTestRule.onNodeWithContentDescription("未签到").performClick()
    composeTestRule.onNodeWithText("每日签到").assertExists()
    composeTestRule.onNodeWithText("今天已经签到").assertExists()
    composeTestRule.runOnIdle { org.junit.Assert.assertEquals(1, checks) }
  }

  @Test
  fun boardNavigationAndTabSwitchPreserveCategoryAndScrollPosition() {
    var boardLoads = 0
    val repository = object : NgaReadOnlyRepository by fakeRepository() {
      override suspend fun loadBoards(session: LoginSessionData?): Result<BoardsReadData> {
        boardLoads++
        return Result.success(BoardsReadData(
          subscribedBoards = emptyList(),
          remoteSections = listOf(NgaBoardSection(
            id = "category", name = "Retained category",
            groups = listOf(NgaBoardGroup(
              id = "group", name = "Group",
              boards = (0..100).map { NgaBoardSummary(boardId = "$it", name = "Board $it") },
            )),
          )),
        ))
      }
    }
    composeTestRule.setContent { MainScreen(repository = repository) }
    waitUntilTextExists("Retained category")
    composeTestRule.onNodeWithText("Retained category").performClick()
    composeTestRule.onNode(
      androidx.compose.ui.test.hasScrollToIndexAction() and
        androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(
          androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange,
        ),
    )
      .performScrollToNode(androidx.compose.ui.test.hasText("Board 80"))
    val before = composeTestRule.onNodeWithText("Board 80").getUnclippedBoundsInRoot()
    composeTestRule.onNodeWithText("Board 80").performClick()
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.onNodeWithText("Board 80").assertExists()
    org.junit.Assert.assertEquals(before, composeTestRule.onNodeWithText("Board 80").getUnclippedBoundsInRoot())
    composeTestRule.onNodeWithContentDescription("Messages", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithContentDescription("Home", useUnmergedTree = true).performClick()
    org.junit.Assert.assertEquals(before, composeTestRule.onNodeWithText("Board 80").getUnclippedBoundsInRoot())
    composeTestRule.runOnIdle { org.junit.Assert.assertEquals(1, boardLoads) }
  }

  @Test
  fun mainScreenShowsFourPrimaryTabsOnly() {
    val repository = fakeRepository()
    composeTestRule.setContent { MainScreen(repository = repository) }

    composeTestRule.onNodeWithContentDescription("Home", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithContentDescription("Messages", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithContentDescription("Profile", useUnmergedTree = true).assertExists()
    composeTestRule.onAllNodesWithContentDescription("Boards").assertCountEquals(0)

    composeTestRule.onNodeWithText("首页").assertExists()
    composeTestRule.onNodeWithText("消息").assertExists()
    composeTestRule.onNodeWithText("我的").assertExists()

    composeTestRule.onAllNodesWithText("Settings").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("Notifications").assertCountEquals(0)
  }

  @Test
  fun homeTabShowsBoardFirstContentWithoutAccountActions() {
    composeTestRule.setContent {
      HomeScreen(
        loginSession = null,
        state =
          HomeUiState(
            activeTopics =
              LoadableUiState.Content(
                listOf(
                  TopicPreview(
                    id = "home-topic",
                    title = "Home active topic",
                    board = "Home board",
                    replyCount = 4,
                    lastActive = "now",
                  ),
                ),
              ),
          ),
        onLoginClick = {},
      )
    }

    composeTestRule.onNodeWithText("Yanga").assertExists()
    composeTestRule.onNodeWithText("Search boards and topics").assertExists()
    composeTestRule.onNodeWithText("Active discussions").assertExists()
    composeTestRule.onNodeWithText("Home active topic").assertExists()

    composeTestRule.onAllNodesWithText("Settings").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("Notifications").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("退出登录").assertCountEquals(0)
  }

  @Test
  fun homeScreenRendersContentStateWithoutOldFakeTopic() {
    composeTestRule.setContent {
      HomeScreen(
        loginSession = null,
        state = HomeUiState(
          boards = LoadableUiState.Content(
            listOf(BoardPreview(id = "remote", name = "Remote strategy board", metadata = "Remote metadata", marker = "R")),
          ),
          activeTopics = LoadableUiState.Content(
            listOf(
              TopicPreview(
                id = "remote-topic",
                title = "Remote launch topic",
                board = "Remote topic board",
                replyCount = 12,
                lastActive = "now",
              ),
            ),
          ),
        ),
        onLoginClick = {},
      )
    }

    composeTestRule.onNodeWithText("Remote launch topic").assertExists()
    composeTestRule.onAllNodesWithText("Remote strategy board").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("关于新版客户端首页信息密度的讨论").assertCountEquals(0)
  }

  @Test
  fun homeScreenRendersErrorState() {
    composeTestRule.setContent {
      HomeScreen(
        loginSession = null,
        state = HomeUiState(
          boards = LoadableUiState.Error("Home boards failed"),
          activeTopics = LoadableUiState.Error("Home topics failed"),
        ),
        onLoginClick = {},
      )
    }

    composeTestRule.onNodeWithText("Home topics failed").assertExists()
    composeTestRule.onAllNodesWithText("Home boards failed").assertCountEquals(0)
  }

  @Test
  fun boardsTabShowsForumDiscoveryContent() {
    composeTestRule.setContent { BoardsScreen() }

    composeTestRule.onNodeWithText("YANGA").assertExists()
    composeTestRule.onNodeWithContentDescription("搜索").assertExists()
    composeTestRule.onNodeWithText("收藏").assertExists()
    composeTestRule.onAllNodesWithText("Subscribed boards").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("Full forum directory").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("Manage boards").assertCountEquals(0)
  }

  @Test
  fun boardsScreenRendersLoginRequiredSubscribedAndRemoteCategories() {
    composeTestRule.setContent {
      BoardsScreen(
        state = BoardsUiState(
          subscribedBoards = LoadableUiState.LoginRequired,
          sections = LoadableUiState.Content(
            listOf(
              BoardSectionPreview(
                id = "remote",
                name = "Remote category",
                groups =
                  listOf(
                    BoardGroupPreview(
                      id = "remote-group",
                      name = "Remote group",
                      boards = listOf(BoardPreview("remote", "Remote board", "Remote directory metadata", "R")),
                    ),
                  ),
              ),
            ),
          ),
        ),
      )
    }

    composeTestRule.onNodeWithText("Sign in to load favorite boards").assertExists()
    composeTestRule.onNodeWithText("Remote category").assertExists()
    composeTestRule.onAllNodesWithText("Remote board").assertCountEquals(0)

    composeTestRule.onNodeWithText("Remote category").performClick()

    composeTestRule.onNodeWithText("Remote board").assertExists()
  }

  @Test
  fun boardsScreenSwitchesBetweenFavoriteAndCategoryBoards() {
    composeTestRule.setContent {
      BoardsScreen(
        state = BoardsUiState(
          subscribedBoards = LoadableUiState.Content(
            listOf(BoardPreview("favorite", "Favorite board", "fid: 7", "F")),
          ),
          sections = LoadableUiState.Content(
            listOf(
              BoardSectionPreview(
                id = "games",
                name = "Games",
                groups =
                  listOf(
                    BoardGroupPreview(
                      id = "games-group-a",
                      name = "Action",
                      boards = listOf(BoardPreview("game", "Game board", "Game metadata", "G")),
                    ),
                    BoardGroupPreview(
                      id = "games-group-b",
                      name = "RPG",
                      boards = listOf(BoardPreview("rpg", "RPG board", "RPG metadata", "R")),
                    ),
                  ),
              ),
              BoardSectionPreview(
                id = "life",
                name = "Life",
                groups =
                  listOf(
                    BoardGroupPreview(
                      id = "life-group",
                      name = "Life Group",
                      boards = listOf(BoardPreview("life", "Life board", "Life metadata", "L")),
                    ),
                  ),
              ),
            ),
          ),
        ),
      )
    }

    composeTestRule.onNodeWithText("收藏").assertExists()
    composeTestRule.onNodeWithText("Favorite board").assertExists()
    composeTestRule.onAllNodesWithText("fid: 7").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("Game board").assertCountEquals(0)

    composeTestRule.onNodeWithText("Games").performClick()

    composeTestRule.onNodeWithText("Action").assertExists()
    composeTestRule.onNodeWithText("RPG").assertExists()
    composeTestRule.onNodeWithText("Game board").assertExists()
    composeTestRule.onNodeWithText("RPG board").assertExists()
  }

  @Test
  fun messagesTabShowsPrivateMessageContextOnly() {
    val repository = fakeRepository()
    composeTestRule.setContent { MainScreen(repository = repository) }

    composeTestRule.onNodeWithContentDescription("Messages", useUnmergedTree = true).performClick()

    composeTestRule.onNodeWithText("Private messages").assertExists()
    composeTestRule.onNodeWithText("Write private message").assertExists()
    composeTestRule.onNodeWithText("Block list").assertExists()
    composeTestRule.onNodeWithText("Unread").assertExists()
    composeTestRule.onNodeWithText("Sent").assertExists()

    composeTestRule.onAllNodesWithText("Reply alerts").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("Favorite topic updates").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("System notifications").assertCountEquals(0)
  }

  @Test
  fun loggedOutProfileTabShowsLoginNotificationAndSettingsContent() {
    val repository = fakeRepository()
    composeTestRule.setContent { MainScreen(repository = repository) }

    composeTestRule.onNodeWithContentDescription("Profile", useUnmergedTree = true).performClick()

    composeTestRule.onNodeWithText("当前未登录").assertExists()
    composeTestRule.onAllNodesWithText("Sign in to load profile").assertCountEquals(0)
    composeTestRule.onNodeWithText("登录").assertExists()
    val loggedOutTitleBounds = composeTestRule.onNodeWithText("当前未登录").getUnclippedBoundsInRoot()
    val loginButtonBounds = composeTestRule.onNodeWithText("登录").getUnclippedBoundsInRoot()
    assertTrue(loginButtonBounds.top < loggedOutTitleBounds.bottom && loginButtonBounds.bottom > loggedOutTitleBounds.top)
    composeTestRule.onAllNodesWithText("主题").assertCountEquals(2)
    composeTestRule.onNodeWithText("设置端点").assertExists()
    composeTestRule.onNodeWithText("账号").assertExists()
    composeTestRule.onNodeWithText("账号设置").assertExists()
    composeTestRule.onAllNodesWithText("点击进行每日签到").assertCountEquals(0)
  }

  @Test
  fun profileAboutRowShowsInstalledVersionAndUpdateAction() {
    composeTestRule.setContent { MainScreen(repository = fakeRepository()) }
    composeTestRule.onNodeWithContentDescription("Profile", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithContentDescription("关于设置入口").performClick()
    composeTestRule.onNodeWithText("关于").assertExists()
    composeTestRule.onNodeWithText("当前版本：", substring = true).assertExists()
    composeTestRule.onNodeWithText("检查更新").assertExists()
    composeTestRule.onNodeWithText("GitHub 项目主页").assertExists()
    composeTestRule.onAllNodesWithText("我的").assertCountEquals(0)
    composeTestRule.onNodeWithContentDescription("返回").performClick()
    composeTestRule.onNodeWithContentDescription("关于设置入口").assertExists()
  }

  @Test
  fun profileThemeRowOpensIndependentThemeSettingsPage() {
    val repository = fakeRepository()
    val preferences = androidx.compose.runtime.mutableStateOf(com.yanga.client.theme.ThemePreferences())
    composeTestRule.setContent {
      MainScreen(
        repository = repository,
        themePreferences = preferences.value,
        onThemePreferencesChange = { preferences.value = it },
      )
    }

    composeTestRule.onNodeWithContentDescription("Profile", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithContentDescription("主题设置入口").performScrollTo().performClick()
    waitUntilTextExists("主题设置")

    composeTestRule.onNodeWithContentDescription("返回").assertExists()
    composeTestRule.onNodeWithText("主题设置").assertExists()
    composeTestRule.onNodeWithText("深色模式").assertExists()
    composeTestRule.onNodeWithText("跟随系统").assertExists()
    composeTestRule.onNodeWithText("浅色").assertExists()
    composeTestRule.onNodeWithText("深色").assertExists()
    composeTestRule.onNodeWithText("主题色").assertExists()
    composeTestRule.onNodeWithText("动态取色").assertExists()
    composeTestRule.onAllNodesWithText("桌面色").assertCountEquals(0)
    composeTestRule.onAllNodesWithContentDescription("桌面主题色").assertCountEquals(0)

    composeTestRule.onNodeWithText("动态取色").performScrollTo()
    if (composeTestRule.onAllNodes(
        androidx.compose.ui.test.isToggleable() and androidx.compose.ui.test.isOn(),
      ).fetchSemanticsNodes().isNotEmpty()) {
      composeTestRule.onNodeWithText("动态取色").performClick()
    }

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithContentDescription("预设主题色 Yanga")
        .fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithContentDescription("预设主题色 Yanga").assertExists()
    composeTestRule.onNodeWithContentDescription("已选主题色").assertExists()
    composeTestRule.onAllNodesWithText("首页").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("消息").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("我的").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("Yanga").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("海蓝").assertCountEquals(0)
  }

  @Test
  fun loggedInProfileTabShowsRedesignedAccountSummaryAndActions() {
    composeTestRule.setContent {
      ProfileScreen(
        loginSession = LoginSessionUiState(
          username = "测试用户",
          uid = "42",
          cookie = "ngaPassportUid=42; ngaPassportCid=abc",
        ),
        state = ProfileUiState(
          session = LoadableUiState.Content(
            LoginSessionData(
              username = "测试用户",
              uid = "42",
              cookie = "ngaPassportUid=42; ngaPassportCid=abc",
              avatarUrl = "https://img4.nga.178.com/avatars/test.jpg",
            ),
          ),
          counters = LoadableUiState.LoginRequired,
          notifications = LoadableUiState.LoginRequired,
        ),
        onLoginClick = {},
        onLogout = {},
      )
    }

    composeTestRule.onAllNodesWithText("已登录").assertCountEquals(0)
    composeTestRule.onNodeWithText("测试用户").assertExists()
    composeTestRule.onNodeWithText("UID 42").assertExists()
    composeTestRule.onNodeWithContentDescription("切换账号").performClick()
    composeTestRule.onAllNodesWithText("已登录账号").assertCountEquals(0)
    composeTestRule.onAllNodesWithContentDescription("测试用户").assertCountEquals(2)
    composeTestRule.onNodeWithContentDescription("当前账号").assertExists()
    composeTestRule.onNodeWithText("添加账号").assertExists()
    composeTestRule.onAllNodesWithText("主题").assertCountEquals(2)
    composeTestRule.onNodeWithText("回复").assertExists()
    composeTestRule.onNodeWithText("通知").assertExists()
    composeTestRule.onNodeWithText("账号").assertExists()
    composeTestRule.onNodeWithText("账号设置").assertExists()
    composeTestRule.onAllNodesWithText("点击进行每日签到").assertCountEquals(0)
    composeTestRule.onNodeWithText("设置端点").assertExists()
    composeTestRule.onNodeWithText("退出登录").assertExists()
    composeTestRule.onAllNodesWithText("Switch account").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("Check in").assertCountEquals(0)
  }

  @Test
  fun loggedOutMessagesScreenShowsLoginRequiredStateWithoutFakeContacts() {
    composeTestRule.setContent {
      MessagesScreen(
        loginSession = null,
        state = MessagesUiState(messages = LoadableUiState.LoginRequired),
        onLoginClick = {},
      )
    }

    composeTestRule.onNodeWithText("Sign in to load private messages").assertExists()
    composeTestRule.onAllNodesWithText("夜航船").assertCountEquals(0)
  }

  @Test
  fun loggedInMessagesScreenShowsRemoteMessageContent() {
    composeTestRule.setContent {
      MessagesScreen(
        loginSession = LoginSessionUiState(
          username = "测试用户",
          uid = "42",
          cookie = "ngaPassportUid=42; ngaPassportCid=abc",
        ),
        state = MessagesUiState(
          messages = LoadableUiState.Content(
            listOf(
              MessagePreview(
                contact = "Remote Contact",
                preview = "Remote private message preview",
                time = "now",
                badge = "2",
              ),
            ),
          ),
        ),
        onLoginClick = {},
      )
    }

    composeTestRule.onNodeWithText("Remote Contact").assertExists()
    composeTestRule.onNodeWithText("Remote private message preview").assertExists()
  }

  @Test
  fun profileScreenShowsStateCountersAndNotifications() {
    composeTestRule.setContent {
      ProfileScreen(
        loginSession = LoginSessionUiState(
          username = "测试用户",
          uid = "42",
          cookie = "ngaPassportUid=42; ngaPassportCid=abc",
        ),
        state = ProfileUiState(
          session = LoadableUiState.Content(
            LoginSessionData(
              username = "测试用户",
              uid = "42",
              cookie = "ngaPassportUid=42; ngaPassportCid=abc",
            ),
          ),
          counters = LoadableUiState.Content(
            listOf(
              SettingsPreview("收", "Remote favorites", "Synced favorite topics", "8"),
              SettingsPreview("版", "Remote boards", "Synced subscribed boards", "5"),
            ),
          ),
          notifications = LoadableUiState.Content(
            listOf(
              SettingsPreview("通", "Remote notification", "Remote reply alert", "3"),
            ),
          ),
        ),
        onLoginClick = {},
        onLogout = {},
      )
    }

    composeTestRule.onNodeWithText("Remote favorites").assertExists()
    composeTestRule.onNodeWithText("8").assertExists()
    composeTestRule.onNodeWithText("Remote boards").assertExists()
    composeTestRule.onNodeWithText("5").assertExists()
    composeTestRule.onNodeWithText("账号设置").assertExists()
    composeTestRule.onNodeWithText("设置端点").assertExists()
  }

  @Test
  fun profileScreenShowsErrorStateText() {
    composeTestRule.setContent {
      ProfileScreen(
        loginSession = LoginSessionUiState(
          username = "测试用户",
          uid = "42",
          cookie = "ngaPassportUid=42; ngaPassportCid=abc",
        ),
        state = ProfileUiState(
          session = LoadableUiState.Content(
            LoginSessionData(
              username = "测试用户",
              uid = "42",
              cookie = "ngaPassportUid=42; ngaPassportCid=abc",
            ),
          ),
          counters = LoadableUiState.Error("Could not load profile counters"),
          notifications = LoadableUiState.Error("Could not load notifications"),
        ),
        onLoginClick = {},
        onLogout = {},
      )
    }

    composeTestRule.onNodeWithText("Could not load profile counters").assertExists()
    composeTestRule.onNodeWithText("账号设置").assertExists()
    composeTestRule.onNodeWithText("设置端点").assertExists()
  }

  @Test
  fun mainScreenRendersInjectedRepositoryDataAcrossTabs() {
    val repository = fakeRepository()
    composeTestRule.setContent {
      MainScreen(
        loginSession = LoginSessionUiState(
          username = "远端测试用户",
          uid = "4242",
          cookie = "ngaPassportUid=4242; ngaPassportCid=fake",
        ),
        repository = repository,
      )
    }

    composeTestRule.onNodeWithContentDescription("Home", useUnmergedTree = true).assertExists()
    composeTestRule.onAllNodesWithText("关于新版客户端首页信息密度的讨论").assertCountEquals(0)

    composeTestRule.onNodeWithContentDescription("Messages", useUnmergedTree = true).performClick()
    waitUntilTextExists("Injected Contact")
    waitUntilTextExists("Injected private message preview")

    composeTestRule.onNodeWithContentDescription("Profile", useUnmergedTree = true).performClick()
    waitUntilTextExists("远端测试用户")
    composeTestRule.onNodeWithText("UID 4242").assertExists()
    composeTestRule.onAllNodesWithText("主题").assertCountEquals(2)
    composeTestRule.onNodeWithText("通知").assertExists()
  }

  private fun waitUntilTextExists(text: String) {
    val found = runCatching {
      composeTestRule.waitUntil(timeoutMillis = 5_000) {
        runCatching {
          composeTestRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)
      }
    }.isSuccess
    if (!found) {
      throw AssertionError("Could not find '$text'. Semantics tree:\n${composeTestRule.onRoot().printToString()}")
    }
  }

  private fun fakeRepository(): NgaReadOnlyRepository =
    object : NgaReadOnlyRepository {
      override suspend fun loadHome(): Result<HomeReadData> =
        Result.success(
          HomeReadData(
            boards = listOf(injectedHomeBoard),
            activeTopics = listOf(injectedTopic),
          ),
        )

      override suspend fun loadBoards(session: LoginSessionData?): Result<BoardsReadData> =
        Result.success(
          BoardsReadData(
            subscribedBoards = listOf(injectedSubscribedBoard),
            remoteSections = listOf(injectedRemoteSection),
          ),
        )

      override suspend fun loadMessages(session: LoginSessionData?): Result<MessagesReadData> =
        Result.success(
          MessagesReadData(
            messages = listOf(injectedMessage),
          ),
        )

      override suspend fun loadProfile(session: LoginSessionData?): Result<ProfileReadData> =
        Result.success(
          ProfileReadData(
            counters = injectedCounters,
            notifications = listOf(injectedNotification),
          ),
        )

      override suspend fun loadBoardTopics(
        session: LoginSessionData?,
        fid: String,
        page: Int,
        fidGroup: String?,
        recommend: Boolean,
      ): Result<NgaTopicList> =
        Result.success(NgaTopicList(topics = emptyList(), page = page, hasNextPage = false))

      override suspend fun loadThread(session: LoginSessionData?, tid: String, page: Int): Result<NgaThreadRead> =
        Result.success(NgaThreadRead(tid = tid, subject = "", fid = "", page = page, posts = emptyList()))

      override suspend fun loadThreadPost(session: LoginSessionData?, pid: String) =
        Result.failure<com.yanga.client.api.NgaThreadPost>(UnsupportedOperationException())

      override suspend fun listLocalFavoriteBoards(): Result<List<LocalFavoriteBoard>> =
        Result.success(emptyList())

      override suspend fun addLocalFavoriteBoard(board: LocalFavoriteBoard): Result<Unit> =
        Result.success(Unit)

      override suspend fun removeLocalFavoriteBoard(boardId: String): Result<Unit> =
        Result.success(Unit)

      override suspend fun refreshIncrementalBoardDirectoryIfDue(): Boolean = false

      override suspend fun loadBlockedSubBoards(session: LoginSessionData?, parentFid: String): Result<Set<String>> =
        Result.success(emptySet())

      override suspend fun applySubBoardVisibilityChanges(
        session: LoginSessionData?,
        parentFid: String,
        changes: List<com.yanga.client.data.SubBoardVisibilityChange>,
      ): Result<Unit> = Result.success(Unit)
    }

  private val injectedHomeBoard =
    NgaBoardSummary(
      boardId = "home-board",
      name = "Injected home board",
    )

  private val injectedRemoteSection =
    NgaBoardSection(
      id = "remote-category",
      name = "Injected remote category",
      groups =
        listOf(
          NgaBoardGroup(
            id = "remote-group",
            name = "Injected group",
            boards = listOf(NgaBoardSummary(boardId = "remote-board", name = "Injected remote board")),
          ),
        ),
    )

  private val injectedSubscribedBoard =
    NgaBoardSummary(
      boardId = "subscribed-board",
      name = "Injected subscribed board",
    )

  private val injectedTopic =
    NgaTopicSummary(
      topicId = "topic-1",
      boardId = "home-board",
      boardName = "Injected home board",
      title = "Injected remote topic",
      authorName = "Remote Author",
      replyCount = 31,
      lastPostAt = 1_771_000_000,
    )

  private val injectedMessage =
    NgaMessageSummary(
      messageId = "message-1",
      contactName = "Injected Contact",
      subject = "Injected subject",
      preview = "Injected private message preview",
      unreadCount = 4,
    )

  private val injectedCounters =
    NgaProfileCounters(
      topicCount = 17,
      replyCount = 31,
      favoriteTopics = 17,
      subscribedBoards = 9,
      unreadNotifications = 5,
      unreadMessages = 4,
    )

  private val injectedNotification =
    NgaNotificationSummary(
      id = "notification-1",
      title = "Injected notification",
      preview = "Injected reply alert",
      unreadCount = 5,
    )
}
