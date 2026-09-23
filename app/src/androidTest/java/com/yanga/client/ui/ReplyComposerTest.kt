package com.yanga.client.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.yanga.client.api.ReplyMode
import com.yanga.client.api.ReplyTarget
import com.yanga.client.data.DefaultNgaReadOnlyRepository
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReplyComposerTest {
  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
  private val login = LoginSessionUiState("Test", "1", "cookie")

  @Test fun sharedEditorOffersQuoteFormattingPreviewAndPreservesDraft() {
    val model = TopicComposerViewModel(SavedStateHandle(mapOf("replyTarget" to ReplyTarget("123", "25", "回复 #2 Test", "被引用内容"))))
    rule.setContent { MaterialTheme { ReplyComposer("测试主题", model, DefaultNgaReadOnlyRepository(), login, {}, {}) } }
    rule.onNodeWithText("发送").assertIsNotEnabled()
    rule.onNodeWithText("引用回复").performClick()
    rule.runOnIdle { model.editContent(TextFieldValue("原生回帖测试")); model.applyTool("b") }
    rule.onNodeWithText("发送").assertIsEnabled()
    rule.onNodeWithText("预览").performScrollTo().performClick()
    rule.runOnIdle {
      assertEquals(ReplyMode.Quote, model.replyTarget!!.mode)
      assertTrue(model.replyBody().contains("被引用内容"))
      assertTrue(model.replyBody().contains("原生回帖测试"))
    }
    rule.onNodeWithContentDescription("返回并保留草稿").performClick()
    rule.runOnIdle { assertTrue(model.content.text.contains("原生回帖测试")) }
  }

  @Test fun loggedOutUserKeepsDraftButCannotSend() {
    val model = TopicComposerViewModel(SavedStateHandle(mapOf("replyTarget" to ReplyTarget("123"))))
    rule.setContent { MaterialTheme { ReplyComposer("测试主题", model, DefaultNgaReadOnlyRepository(), null, {}, {}) } }
    rule.runOnIdle { model.editContent(TextFieldValue("保留草稿")) }
    rule.onNodeWithText("发送").assertIsNotEnabled()
    rule.onNodeWithContentDescription("返回并保留草稿").performClick()
    rule.runOnIdle { assertEquals("保留草稿", model.content.text) }
  }

  @Test fun replyDraftRestoresRecipientAndSeparatesAccountsAndThreads() {
    val context = rule.activity
    val account = "reply_test"
    val post = PostPreview(pid = "25", floorNumber = 2, author = "Test", floor = "2", time = "", content = "引用原文", avatarInitial = "T")
    val model = TopicComposerViewModel(SavedStateHandle())
    rule.runOnIdle {
      model.openReply(context, account, "987654", post)
      model.clearDraft()
      model.openReply(context, account, "987654", post)
      model.editContent(TextFieldValue("持久保存的回复"))
      model.changeReplyMode(ReplyMode.Quote)
      model.close()
      val restored = TopicComposerViewModel(SavedStateHandle())
      restored.openReply(context, account, "987654", null)
      assertEquals("25", restored.replyTarget!!.pid)
      assertEquals(ReplyMode.Quote, restored.replyTarget!!.mode)
      assertEquals("持久保存的回复", restored.content.text)
      restored.openReply(context, account, "987655", null)
      assertEquals("", restored.content.text)
      assertEquals("987655", restored.replyTarget!!.tid)
      restored.openReply(context, "reply_other_account", "987654", null)
      assertEquals("", restored.content.text)
      restored.clearDraft()
      model.clearDraft()
    }
  }
}
