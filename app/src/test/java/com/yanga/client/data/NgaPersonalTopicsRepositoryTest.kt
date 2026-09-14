package com.yanga.client.data
import com.yanga.client.api.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
class NgaPersonalTopicsRepositoryTest {
  @Test fun sendsAccountFiltersAndPageWithSession() = runTest {
    val requests = mutableListOf<NgaRequest>()
    val repository = DefaultNgaReadOnlyRepository(object : NgaHttpTransport {
      override fun execute(request: NgaRequest): Result<NgaHttpResponse> {
        requests += request
        return Result.success(NgaHttpResponse(200, """{"data":{"__T":[]}}"""))
      }
    })
    val session = LoginSessionData(uid = "42", username = "user", cookie = "test-cookie")
    assertTrue(repository.loadPersonalTopics(null, NgaPersonalTopicKind.Topics, 1).isFailure)
    assertTrue(requests.isEmpty())
    for (kind in NgaPersonalTopicKind.entries) repository.loadPersonalTopics(session, kind, 3).getOrThrow()
    assertEquals("42", requests[0].query["authorid"])
    assertNull(requests[0].query["searchpost"])
    assertEquals("1", requests[1].query["searchpost"])
    assertEquals("1", requests[2].query["favor"])
    assertNull(requests[2].query["authorid"])
    requests.forEach { assertEquals("3", it.query["page"]); assertEquals("test-cookie", it.headers["Cookie"]) }
    repository.loadUserTopics(session, "99", 2).getOrThrow()
    assertEquals("99", requests.last().query["authorid"])
    assertEquals("2", requests.last().query["page"])
    assertNull(requests.last().query["searchpost"])
    assertTrue(repository.loadUserTopics(session, "-1", 1).isFailure)

  }
}
