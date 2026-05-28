package com.yanga.client.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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
  fun loggedOutStateShowsLoginAction() {
    composeTestRule.onNodeWithText("Yanga").assertExists()
    composeTestRule.onNodeWithText("当前未登录").assertExists()
    composeTestRule.onNodeWithText("登录 NGA").assertExists()
  }

  @Test
  fun loggedInStateShowsAccountActions() {
    composeTestRule.setContent {
      MainScreen(
        loginSession = LoginSessionUiState(
          username = "测试用户",
          uid = "42",
          cookie = "ngaPassportUid=42; ngaPassportCid=abc",
        ),
      )
    }

    composeTestRule.onNodeWithText("已登录").assertExists()
    composeTestRule.onNodeWithText("测试用户").assertExists()
    composeTestRule.onNodeWithText("UID 42").assertExists()
    composeTestRule.onNodeWithText("退出登录").assertExists()
  }
}
