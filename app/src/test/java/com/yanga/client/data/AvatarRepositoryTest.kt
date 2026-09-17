package com.yanga.client.data

import com.yanga.client.api.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AvatarRepositoryTest {
  private class Transport(vararg responses: String) : NgaHttpTransport {
    val replies = ArrayDeque(responses.toList())
    val requests = mutableListOf<NgaRequest>()
    override fun execute(request: NgaRequest): Result<NgaHttpResponse> {
      requests += request
      return Result.success(NgaHttpResponse(200, replies.removeFirst()))
    }
  }
  private val ticket = """{"data":{"0":"old","1":"token","2":"https://img.nga.cn/attach.php?checksum=token&uid=42"}}"""
  private fun session() = LoginSessionData(uid = "42", username = "reader", cookie = "ngaPassportUid=42")

  @Test fun uploadAndSaveFollowOfficialProtocol() = runTest {
    val avatar = "\t.a/42_0.png?13\tnew\t"
    val transport = Transport(ticket, """{"data":{"0":{"url":"\t.a/42_0.png?13\tnew\t"}}}""", """{"data":{"0":"操作成功"}}""")
    val repository = DefaultNgaReadOnlyRepository(transport)
    val result = repository.uploadAvatar(session(), byteArrayOf(0, 1, 127, -1)).getOrThrow()
    assertEquals(avatar, result)
    repository.saveAvatar(session(), result).getOrThrow()
    assertEquals("get", transport.requests[0].query["__act"])
    assertEquals("1", transport.requests[0].query["edit"])
    val upload = transport.requests[1]
    assertEquals("https://img.nga.cn/attach.php?uid=42", upload.url)
    assertTrue(upload.headers.keys.none { it.equals("Cookie", true) })
    val body = upload.binaryBody!!.toString(Charsets.UTF_8)
    assertTrue(body.contains("name=\"n42_0\"\r\n\r\ndata:image/png;base64,AAF//w==\r\n"))
    assertTrue(body.contains("name=\"checksum\"\r\n\r\ntoken\r\n"))
    val save = transport.requests[2]
    assertEquals("set_avatar", save.bodyMap["__lib"])
    assertEquals("set", save.bodyMap["__act"])
    assertEquals(avatar, NgaEncoding.urlDecodeGbk(save.bodyMap.getValue("avatar")))
  }

  @Test fun loginAndImageValidationPreventRequests() = runTest {
    val transport = Transport()
    val repository = DefaultNgaReadOnlyRepository(transport)
    assertTrue(repository.uploadAvatar(null, byteArrayOf(1)).isFailure)
    assertTrue(repository.uploadAvatar(session(), byteArrayOf()).isFailure)
    assertTrue(repository.saveAvatar(null, "avatar").isFailure)
    assertTrue(repository.saveAvatar(session(), "").isFailure)
    assertTrue(transport.requests.isEmpty())
  }

  @Test fun preflightFailureDoesNotUpload() = runTest {
    for (reply in listOf("{}", "<html>error</html>", """{"error":"禁止修改头像"}""",
      ticket.replace("img.nga.cn", "untrusted.example"))) {
      val transport = Transport(reply)
      assertTrue(DefaultNgaReadOnlyRepository(transport).uploadAvatar(session(), byteArrayOf(1)).isFailure)
      assertEquals(1, transport.requests.size)
    }
  }

  @Test fun malformedUploadAndRejectedSaveAreFailures() = runTest {
    val transport = Transport(ticket, "{}", """{"error":"图片不合法"}""")
    val repository = DefaultNgaReadOnlyRepository(transport)
    assertTrue(repository.uploadAvatar(session(), byteArrayOf(1)).isFailure)
    assertTrue(repository.saveAvatar(session(), "avatar").isFailure)
  }
}
