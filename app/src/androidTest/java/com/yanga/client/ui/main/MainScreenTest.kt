package com.yanga.client.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [MainScreen]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    composeTestRule.setContent { MainScreen() }
  }

  @Test
  fun mainScreenShowsFourPrimaryTabsOnly() {
    composeTestRule.onNodeWithText("Home").assertExists()
    composeTestRule.onNodeWithText("Boards").assertExists()
    composeTestRule.onNodeWithText("Messages").assertExists()
    composeTestRule.onNodeWithText("Profile").assertExists()

    composeTestRule.onAllNodesWithText("Settings").assertCountEquals(0)
    composeTestRule.onAllNodesWithText("Notifications").assertCountEquals(0)
  }

  @Test
  fun homeTabShowsBoardFirstContentWithoutAccountActions() {
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
  fun boardsTabShowsForumDiscoveryContent() {
    composeTestRule.onNodeWithText("Boards").performClick()

    composeTestRule.onNodeWithText("Board search").assertExists()
    composeTestRule.onNodeWithText("Subscribed boards").assertExists()
    composeTestRule.onNodeWithText("Full forum directory").assertExists()
    composeTestRule.onNodeWithText("Manage boards").assertExists()
  }

  @Test
  fun messagesTabShowsPrivateMessageContextOnly() {
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
    composeTestRule.onNodeWithText("Profile").performClick()

    composeTestRule.onNodeWithText("当前未登录").assertExists()
    composeTestRule.onNodeWithText("登录 NGA").assertExists()
    composeTestRule.onNodeWithText("Notifications").assertExists()
    composeTestRule.onNodeWithText("Reading and appearance").assertExists()
    composeTestRule.onNodeWithText("Cache and history").assertExists()
    composeTestRule.onNodeWithText("Block words").assertExists()
  }

  @Test
  fun loggedInProfileTabShowsAccountActions() {
    composeTestRule.setContent {
      MainScreen(
        loginSession = LoginSessionUiState(
          username = "测试用户",
          uid = "42",
          cookie = "ngaPassportUid=42; ngaPassportCid=abc",
        ),
      )
    }

    composeTestRule.onNodeWithText("Profile").performClick()

    composeTestRule.onNodeWithText("已登录").assertExists()
    composeTestRule.onNodeWithText("测试用户").assertExists()
    composeTestRule.onNodeWithText("UID 42").assertExists()
    composeTestRule.onNodeWithText("Switch account").assertExists()
    composeTestRule.onNodeWithText("退出登录").assertExists()
    composeTestRule.onNodeWithText("Notifications").assertExists()
    composeTestRule.onNodeWithText("Settings").assertExists()
  }
}
