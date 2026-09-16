package com.yanga.client.data

import com.yanga.client.api.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NgaPollRepositoryTest {
  private val poll = NgaPoll("10", listOf(NgaPollOption(101, "A", 0), NgaPollOption(205, "B", 0)), 1)
  private val session = LoginSessionData("Test", "1", "test-cookie")

  @Test fun submissionRequiresLoginAndValidSelectionBeforeSending() = runTest {
    val transport = Transport()
    val repository = DefaultNgaReadOnlyRepository(transport)
    assertTrue(repository.submitPoll(null, poll, listOf(101)).isFailure)
    assertTrue(repository.submitPoll(session, poll, listOf(999)).isFailure)
    assertTrue(repository.submitPoll(session, poll.copy(endsAt = 1), listOf(101)).isFailure)
    assertTrue(repository.submitPoll(session, poll.copy(isBet = true), listOf(101)).isFailure)
    assertTrue(transport.requests.isEmpty())
  }

  @Test fun sendsRealOptionIdAndPropagatesServerRejectionWithoutRetry() = runTest {
    val transport = Transport()
    val repository = DefaultNgaReadOnlyRepository(transport)
    assertTrue(repository.submitPoll(session, poll, listOf(205)).isSuccess)
    val request = transport.requests.single()
    assertEquals(NgaHttpMethod.POST, request.method)
    assertEquals("205", request.query["voteid"])
    assertEquals("vote", request.query["__act"])
    assertEquals("test-cookie", request.headers["Cookie"])
    transport.response = """{"error":{"0":"已经投票"}}"""
    val failure = repository.submitPoll(session, poll, listOf(101))
    assertTrue(failure.isFailure)
    assertEquals("已经投票", failure.exceptionOrNull()?.message)
    assertEquals(2, transport.requests.size)
  }

  private class Transport : NgaHttpTransport {
    var response = """{"data":{"0":"操作成功"}}"""
    val requests = mutableListOf<NgaRequest>()
    override fun execute(request: NgaRequest): Result<NgaHttpResponse> {
      requests += request
      return Result.success(NgaHttpResponse(200, response))
    }
  }
}
