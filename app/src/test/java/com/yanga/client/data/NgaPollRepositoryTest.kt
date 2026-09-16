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

  @Test fun confirmedVoteIsRestoredWhenReopeningThreadAndIsAccountScoped() = runTest {
    val store = PollVoteStore()
    val transport = Transport()
    val repository = DefaultNgaReadOnlyRepository(transport, pollVoteStore = store)
    assertTrue(repository.submitPoll(session, poll, listOf(205)).isSuccess)
    transport.response = """{"data":{"__T":{"tid":10},"__R":{"0":{"pid":0,"content":"正文","vote":"101~A~205~B~max_select~1~_101~0~_205~1"}}}}"""
    val reopened = DefaultNgaReadOnlyRepository(transport, pollVoteStore = store)
    val restored = reopened.loadThread(session, "10", 1).getOrThrow().posts.single().poll!!
    assertTrue(restored.hasVoted)
    assertEquals(listOf(205), restored.votedOptionIds)
    assertEquals("已提交投票", restored.validationError(listOf(101)))
    assertFalse(reopened.loadThread(session.copy(uid = "2"), "10", 1).getOrThrow().posts.single().poll!!.hasVoted)
    assertFalse(reopened.loadThread(null, "10", 1).getOrThrow().posts.single().poll!!.hasVoted)
  }

  @Test fun rejectedVoteIsNotRecorded() = runTest {
    val store = PollVoteStore()
    val transport = Transport().apply { response = """{"error":{"0":"失败"}}""" }
    assertTrue(DefaultNgaReadOnlyRepository(transport, pollVoteStore = store).submitPoll(session, poll, listOf(101)).isFailure)
    assertNull(store.selection(session.uid, poll.tid))
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
