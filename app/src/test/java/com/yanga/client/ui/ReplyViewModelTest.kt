package com.yanga.client.ui

import androidx.lifecycle.SavedStateHandle
import com.yanga.client.api.NgaReplyParser
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
  @Test fun confirmedReplyClosesComposerAndClearsDraftWhileRejectionKeepsIt() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
      var response = """{"data":{"__MESSAGE":{"1":"发贴完毕","3":200}}}"""
      var calls = 0
      val repository = object : NgaReadOnlyRepository by DefaultNgaReadOnlyRepository() {
        override suspend fun submitReply(session: LoginSessionData?, tid: String, pid: String, content: String): Result<Unit> {
          calls++
          return runCatching { NgaReplyParser.requireSuccess(response) }
        }
      }
      val model = ReplyViewModel(SavedStateHandle())
      model.open(null)
      model.edit("回复内容")
      model.submit(repository, null, "123")
      model.submit(repository, null, "123")
      advanceUntilIdle()
      assertEquals(1, calls)
      assertFalse(model.visible.value)
      assertEquals("", model.content.value)
      assertTrue(model.sent.value)
      assertFalse(model.sending.value)

      model.sent.value = false
      response = """{"error":{"0":"主题已锁定"}}"""
      model.open(null)
      model.edit("保留草稿")
      model.submit(repository, null, "123")
      advanceUntilIdle()
      assertTrue(model.visible.value)
      assertEquals("保留草稿", model.content.value)
      assertFalse(model.sent.value)
      assertFalse(model.sending.value)
    } finally { Dispatchers.resetMain() }
  }
}
