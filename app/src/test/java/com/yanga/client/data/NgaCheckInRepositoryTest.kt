package com.yanga.client.data

import com.yanga.client.api.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NgaCheckInRepositoryTest {
  @Test fun queriesStatusWithActiveSessionWithoutPerformingCheckIn() = runTest {
    val requests = mutableListOf<NgaRequest>()
    val repo = DefaultNgaReadOnlyRepository(object : NgaHttpTransport {
      override fun execute(request: NgaRequest): Result<NgaHttpResponse> {
        requests += request
        return Result.success(NgaHttpResponse(200, """{"data":{"0":{"last_day":20719,"now_day":20719}}}"""))
      }
    })
    assertTrue(repo.loadCheckInStatus(null).isFailure)
    assertTrue(requests.isEmpty())
    assertEquals(true, repo.loadCheckInStatus(LoginSessionData("Test", "1", "test-cookie")).getOrThrow())
    val request = requests.single()
    assertEquals(NgaHttpMethod.GET, request.method)
    assertEquals("test-cookie", request.headers["Cookie"])
    assertEquals("check_in", request.query["__lib"])
    assertEquals("get_stat", request.query["__act"])
    assertEquals("8", request.query["__output"])
  }
}
