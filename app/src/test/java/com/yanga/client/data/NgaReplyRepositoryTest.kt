package com.yanga.client.data

import com.yanga.client.api.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NgaReplyRepositoryTest {
  private val session = LoginSessionData("Test", "1", "cookie")
  private class Transport : NgaHttpTransport {
    val requests = mutableListOf<NgaRequest>()
    var response = """{"data":{"0":"发贴完毕"}}"""
    override fun execute(request: NgaRequest): Result<NgaHttpResponse> {
      requests += request
      return Result.success(NgaHttpResponse(200, response))
    }
  }

  @Test fun validatesBeforeSending() = runTest {
    val transport = Transport()
    val repo = DefaultNgaReadOnlyRepository(transport)
    assertTrue(repo.submitReply(null, "10", "0", "你好").isFailure)
    assertTrue(repo.submitReply(session, "10", "0", "  ").isFailure)
    assertTrue(repo.submitReply(session, "bad", "0", "你好").isFailure)
    assertTrue(repo.submitReply(session, "10", "-1", "你好").isFailure)
    assertTrue(transport.requests.isEmpty())
  }

  @Test fun sendsAuthenticatedGbkReplyToSelectedFloor() = runTest {
    val transport = Transport()
    val repo = DefaultNgaReadOnlyRepository(transport)
    assertTrue(repo.submitReply(session, "10", "25", "中文 & +\n回复").isSuccess)
    val request = transport.requests.single()
    assertEquals(NgaHttpMethod.POST, request.method)
    assertEquals("cookie", request.headers["Cookie"])
    assertEquals("reply", request.bodyMap["action"])
    assertEquals("10", request.bodyMap["tid"])
    assertEquals("25", request.bodyMap["pid"])
    assertEquals("2", request.bodyMap["step"])
    assertEquals("中文 & +\n回复", NgaEncoding.urlDecodeGbk(request.bodyMap.getValue("post_content")))
  }

  @Test fun rejectionAndUnconfirmedResponseDoNotRetry() = runTest {
    val transport = Transport()
    val repo = DefaultNgaReadOnlyRepository(transport)
    transport.response = """{"error":{"0":"主题已锁定"}}"""
    assertEquals("主题已锁定", repo.submitReply(session, "10", "0", "内容").exceptionOrNull()?.message)
    transport.response = """{"data":{}}"""
    assertTrue(repo.submitReply(session, "10", "0", "内容").isFailure)
    assertEquals(2, transport.requests.size)
  }
}
