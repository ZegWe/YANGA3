package com.yanga.client.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.yanga.client.ui.main.MainScreen
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
  fun blankShellShowsPendingUiMessage() {
    composeTestRule.onNodeWithText("Yanga").assertExists()
    composeTestRule.onNodeWithText("API layer first. UI organization is pending.").assertExists()
  }
}
