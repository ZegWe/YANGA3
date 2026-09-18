package com.yanga.client.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReplyComposerTest {
  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  @Test fun editorValidatesInputAndOffersExplicitWebAction() {
    val model = ReplyViewModel(SavedStateHandle())
    var webClicks = 0
    var sendClicks = 0
    rule.setContent {
      MaterialTheme {
        ReplyComposer("测试主题", model, true, { sendClicks++ }, { webClicks++ })
      }
    }
    rule.onNodeWithText("发送").assertIsNotEnabled()
    rule.onNodeWithText("回复内容").performTextInput("原生回帖测试")
    rule.onNodeWithText("发送").assertIsEnabled().performClick()
    rule.runOnIdle { assertEquals(1, sendClicks); assertEquals(0, webClicks) }
    rule.onNodeWithText("网页回帖").performClick()
    rule.runOnIdle { assertEquals(1, webClicks); assertEquals("原生回帖测试", model.content.value) }
  }

  @Test fun loggedOutUserKeepsDraftButCannotSend() {
    val model = ReplyViewModel(SavedStateHandle())
    rule.setContent { MaterialTheme { ReplyComposer("测试主题", model, false, {}, {}) } }
    rule.onNodeWithText("回复内容").performTextInput("保留草稿")
    rule.onNodeWithText("发送").assertIsNotEnabled()
    rule.onNodeWithContentDescription("返回并保留草稿").performClick()
    rule.runOnIdle { assertEquals("保留草稿", model.content.value) }
  }
}
