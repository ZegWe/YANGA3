package com.yanga.client.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ThreadRefreshTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test fun pullingDownOnThreadRequestsRefresh() {
    var refreshes = 0
    compose.setContent {
      ThreadReadingScreen(
        state = ThreadUiState(title = "测试主题", posts = LoadableUiState.Content(listOf(
          PostPreview(pid = "1", author = "作者", floor = "0", time = "现在", content = "帖子内容", avatarInitial = "作"),
        ))),
        onBack = {}, onRefresh = { refreshes++ },
      )
    }
    compose.onNodeWithTag("thread-posts-1").performTouchInput { swipeDown() }
    compose.runOnIdle { assertEquals(1, refreshes) }
  }
}
