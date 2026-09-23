package com.yanga.client.ui

import androidx.lifecycle.SavedStateHandle
import com.yanga.client.api.*
import androidx.compose.ui.text.input.TextFieldValue
import com.yanga.client.data.DefaultNgaReadOnlyRepository
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.data.LoginSessionData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReplyViewModelTest {
  @Test fun quoteUsesTargetLinkWithoutInventingPageAndKeepsOriginalEditable() {
    val saved = SavedStateHandle(mapOf("replyTarget" to ReplyTarget("123", "456", "回复 #80 Test", "原始内容", ReplyMode.Quote)))
    val model = TopicComposerViewModel(saved)
    model.editContent(TextFieldValue("我的回复"))
    model.editReplyQuote("引用节选")
    assertEquals("[quote][pid=456,123]回复 #80 Test[/pid]\n引用节选\n[/quote]\n我的回复", model.replyBody())
    model.changeReplyMode(ReplyMode.Comment)
    assertEquals("我的回复", model.replyBody())
    model.changeReplyMode(ReplyMode.Quote)
    assertTrue(TopicComposerViewModel(saved).replyBody().contains("引用节选"))
    val mainPost = TopicComposerViewModel(SavedStateHandle(mapOf("replyTarget" to ReplyTarget("123", quote = "主楼", mode = ReplyMode.Quote))))
    assertTrue(mainPost.replyBody().contains("[tid=123]"))
    assertFalse(mainPost.replyBody().contains("[pid=0"))
  }
  @Test fun confirmedReplyClosesComposerAndClearsDraftWhileRejectionKeepsIt() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
      var response = """{"data":{"__MESSAGE":{"1":"发贴完毕","3":200}}}"""
      var calls = 0
      val repository = object : NgaReadOnlyRepository by DefaultNgaReadOnlyRepository() {
        override suspend fun submitRichReply(session: LoginSessionData?, target: ReplyTarget, subject: String, content: String, attachments: List<TopicAttachment>, options: TopicPostOptions): Result<Unit> {
          calls++
          return runCatching { NgaReplyParser.requireSuccess(response) }
        }
      }
      val model = TopicComposerViewModel(SavedStateHandle(mapOf("replyTarget" to ReplyTarget("123"))))
      model.open()
      model.editContent(TextFieldValue("回复内容"))
      model.submit(repository, LoginSessionData("Test", "1", "cookie"), 0)
      model.submit(repository, LoginSessionData("Test", "1", "cookie"), 0)
      advanceUntilIdle()
      assertEquals(1, calls)
      assertFalse(model.visible)
      assertEquals("", model.content.text)
      assertTrue(model.sent)
      assertFalse(model.busy)

      model.sent = false
      response = """{"error":{"0":"主题已锁定"}}"""
      model.open()
      model.editContent(TextFieldValue("保留草稿"))
      model.submit(repository, LoginSessionData("Test", "1", "cookie"), 0)
      advanceUntilIdle()
      assertTrue(model.visible)
      assertEquals("保留草稿", model.content.text)
      assertFalse(model.sent)
      assertFalse(model.busy)
    } finally { Dispatchers.resetMain() }
  }
}
