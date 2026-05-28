package com.yanga.client.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.yanga.client.data.LoginSessionData
import org.junit.Rule
import org.junit.Test

/** UI tests for [MainScreen]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun mainScreenShowsFourPrimaryTabsOnly() {
    composeTestRule.setContent { MainScreen() }

    composeTestRule.onNodeWithText("Home").assertExists()
    composeTestRule.onNodeWithText("Boards").assertExists()
    composeTestRule.onNodeWithText("Messages").assertExists()
    composeTestRule.onNodeWithText("Profile").assertExists()

    composeTestRule.onAllNodesWithText("Settings").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("Notifications").assertCountEquals(0)
  }

  @Test
  fun homeTabShowsBoardFirstContentWithoutAccountActions() {
    composeTestRule.setContent { MainScreen() }

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
            listOf(BoardPreview("Remote strategy board", "Remote metadata", "R")),
          ),
          activeTopics = LoadableUiState.Content(
            listOf(TopicPreview("Remote launch topic", "Remote topic board", "12 replies", "now", "L")),
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
    composeTestRule.setContent { MainScreen() }

    composeTestRule.onNodeWithText("Boards").performClick()

    composeTestRule.onNodeWithText("Board search").assertExists()
    composeTestRule.onNodeWithText("Subscribed boards").assertExists()
    composeTestRule.onNodeWithText("Full forum directory").assertExists()
    composeTestRule.onNodeWithText("Manage boards").assertExists()
  }

  @Test
  fun boardsScreenRendersLoginRequiredSubscribedAndRemoteCategories() {
    composeTestRule.setContent {
      BoardsScreen(
        state = BoardsUiState(
          subscribedBoards = LoadableUiState.LoginRequired,
          categories = LoadableUiState.Content(
            listOf(BoardPreview("Remote category", "Remote directory metadata", "C")),
          ),
        ),
      )
    }

    composeTestRule.onNodeWithText("Sign in to load subscribed boards").assertExists()
    composeTestRule.onNodeWithText("Remote category").assertExists()
  }

  @Test
  fun messagesTabShowsPrivateMessageContextOnly() {
    composeTestRule.setContent { MainScreen() }

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
    composeTestRule.setContent { MainScreen() }

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
}
