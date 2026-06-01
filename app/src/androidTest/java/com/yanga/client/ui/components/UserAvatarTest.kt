package com.yanga.client.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class UserAvatarTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun userAvatarWithNoAvatarUrlOnlyShowsInitialFallback() {
    composeTestRule.setContent {
      UserAvatar(name = "reader", avatarUrl = null)
    }

    composeTestRule.onNodeWithText("r").assertExists()
    composeTestRule.onAllNodesWithContentDescription("reader").assertCountEquals(0)
  }
}
