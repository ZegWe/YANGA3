package com.yanga.client.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.performClick
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
import org.junit.Rule
import org.junit.Test

/** UI tests for [MainScreen]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun mainScreenShowsFourPrimaryTabsOnly() {
    val repository = fakeRepository()
    composeTestRule.setContent { MainScreen(repository = repository) }

    composeTestRule.onNodeWithText("Home").assertExists()
    composeTestRule.onNodeWithText("Boards").assertExists()
    composeTestRule.onNodeWithText("Messages").assertExists()
    composeTestRule.onNodeWithText("Profile").assertExists()

    composeTestRule.onAllNodesWithText("Settings").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("Notifications").assertCountEquals(0)
  }

  @Test
  fun homeTabShowsBoardFirstContentWithoutAccountActions() {
    val repository = fakeRepository()
    composeTestRule.setContent { MainScreen(repository = repository) }

    composeTestRule.onNodeWithText("Home").performClick()

    composeTestRule.onNodeWithText("Yanga").assertExists()
    composeTestRule.onNodeWithText("Favorite").assertExists()
    composeTestRule.onNodeWithText("Hot topics").assertExists()
    composeTestRule.onNodeWithText("Favorites").assertExists()
    composeTestRule.onNodeWithText("History").assertExists()

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

    composeTestRule.onNodeWithText("Remote strategy board").assertExists()
    composeTestRule.onNodeWithText("Remote launch topic").assertExists()
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

    composeTestRule.onNodeWithText("Home boards failed").assertExists()
    composeTestRule.onNodeWithText("Home topics failed").assertExists()
  }

  @Test
  fun boardsTabShowsForumDiscoveryContent() {
    val repository = fakeRepository()
    composeTestRule.setContent { MainScreen(repository = repository) }

    composeTestRule.onNodeWithText("Boards").performClick()

    composeTestRule.onNodeWithText("Board search").assertExists()
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
    composeTestRule.onAllNodesWithText("Favorite board").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("Life board").assertCountEquals(0)
  }

  @Test
  fun messagesTabShowsPrivateMessageContextOnly() {
    val repository = fakeRepository()
    composeTestRule.setContent { MainScreen(repository = repository) }

    composeTestRule.onNodeWithText("Messages").performClick()

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

    composeTestRule.onNodeWithText("Profile").performClick()

    composeTestRule.onNodeWithText("当前未登录").assertExists()
    composeTestRule.onNodeWithText("登录 NGA").assertExists()
    composeTestRule.onNodeWithText("Sign in to load notifications").assertExists()
    composeTestRule.onNodeWithText("Reading and appearance").assertExists()
    composeTestRule.onNodeWithText("Cache and history").assertExists()
    composeTestRule.onNodeWithText("Block words").assertExists()
  }

  @Test
  fun loggedInProfileTabShowsAccountActions() {
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
          counters = LoadableUiState.LoginRequired,
          notifications = LoadableUiState.LoginRequired,
        ),
        onLoginClick = {},
        onLogout = {},
      )
    }

    composeTestRule.onNodeWithText("已登录").assertExists()
    composeTestRule.onNodeWithText("测试用户").assertExists()
    composeTestRule.onNodeWithText("UID 42").assertExists()
    composeTestRule.onNodeWithText("Switch account").assertExists()
    composeTestRule.onNodeWithText("退出登录").assertExists()
    composeTestRule.onNodeWithText("Sign in to load notifications").assertExists()
    composeTestRule.onNodeWithText("Settings").assertExists()
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
    composeTestRule.onNodeWithText("Remote notification").assertExists()
    composeTestRule.onNodeWithText("Remote reply alert").assertExists()
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
    composeTestRule.onNodeWithText("Could not load notifications").assertExists()
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

    waitUntilTextExists("Injected home board")
    composeTestRule.onNodeWithText("Injected remote topic").assertExists()
    composeTestRule.onAllNodesWithText("关于新版客户端首页信息密度的讨论").assertCountEquals(0)

    composeTestRule.onNodeWithText("Boards").performClick()
    waitUntilTextExists("Injected subscribed board")
    composeTestRule.onNodeWithText("Injected remote category").assertExists()

    composeTestRule.onNodeWithText("Messages").performClick()
    waitUntilTextExists("Injected Contact")
    waitUntilTextExists("Injected private message preview")

    composeTestRule.onNodeWithText("Profile").performClick()
    waitUntilTextExists("远端测试用户")
    composeTestRule.onNodeWithText("UID 4242").assertExists()
    composeTestRule.onNodeWithText("Favorite topics").assertExists()
    composeTestRule.onNodeWithText("17").assertExists()
    composeTestRule.onNodeWithText("Injected notification").assertExists()
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
