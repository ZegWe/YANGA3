package com.yanga.client.api

import com.yanga.client.data.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NgaPostActionsTest {
  @Test fun topicAuthorIsIdentifiedOnLaterPagesAndScoresRemainSigned() {
    val result = NgaThreadParser.parseRead("""{"data":{"__PAGE":2,"__T":{"tid":10,"authorid":42},"__R":{"0":{"pid":21,"lou":20,"authorid":42,"score":7},"1":{"pid":22,"lou":21,"authorid":43,"score":-2}}}}""")
    assertTrue(result.posts[0].isOriginalPoster)
    assertFalse(result.posts[1].isOriginalPoster)
    assertEquals(listOf(7, -2), result.posts.map { it.score })
  }

  @Test fun anonymousPageIdsAreNotMistakenForOriginalPoster() {
    val result = NgaThreadParser.parseRead("""{"data":{"__T":{"tid":10,"authorid":-1},"__R":{"0":{"lou":20,"authorid":-1}}}}""")
    assertFalse(result.posts.single().isOriginalPoster)
  }

  @Test fun serverErrorsAndUnrecognizedResponsesAreNotSuccessfulReactions() {
    NgaReactionParser.requireSuccess("""{"data":{"0":"操作成功"}}""")
    NgaReactionParser.requireSuccess("""{"data":{"0":-2}}""")
    assertTrue(runCatching { NgaReactionParser.requireSuccess("""{"error":{"0":"请先登录"}}""") }.isFailure)
    assertTrue(runCatching { NgaReactionParser.requireSuccess("""{"data":{"0":"不能评价自己"}}""") }.isFailure)
  }

  @Test fun filtersAreSentToServerAndReactionRequiresLogin() = runTest {
    val requests = mutableListOf<NgaRequest>()
    val transport = object : NgaHttpTransport {
      override fun execute(request: NgaRequest): Result<NgaHttpResponse> {
        requests += request
        return Result.success(NgaHttpResponse(200, """{"data":{"0":"操作成功"}}"""))
      }
    }
    val repository = DefaultNgaReadOnlyRepository(transport)
    assertTrue(repository.reactToPost(null, "10", "21", true).isFailure)
    assertTrue(requests.isEmpty())
    repository.loadThreadByAuthor(null, "10", 2, "42")
    assertEquals("42", requests.single().query["authorid"])
    assertEquals("2", requests.single().query["page"])
    assertTrue(repository.reactToPost(LoginSessionData("test", "1", "test-cookie"), "10", "21", false).isSuccess)
    assertEquals("test-cookie", requests.last().headers["Cookie"])
  }
}
