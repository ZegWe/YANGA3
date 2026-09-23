package com.yanga.client.data

import com.yanga.client.api.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NgaReplyRepositoryTest {
  @Test fun replyUploadObtainsReplyPermissionInsteadOfNewTopicPermission() = runTest {
    val requests = mutableListOf<NgaRequest>()
    val repo = DefaultNgaReadOnlyRepository(object : NgaHttpTransport {
      override fun execute(request: NgaRequest): Result<NgaHttpResponse> {
        requests += request
        val response = when (requests.size) {
          1 -> """{"data":{"auth":"reply-ticket"}}"""
          2 -> """{"data":{"__T":{"tid":10,"fid":321}}}"""
          else -> """{"data":{"attachments":"id","attachments_check":"check","url":"mon_202609/321/test.png"}}"""
        }
        return Result.success(NgaHttpResponse(200, response))
      }
    })
    val result = repo.uploadReplyAttachment(session, ReplyTarget("10", "25", mode = ReplyMode.Quote), "test.png", "image/png", byteArrayOf(1, 2, 3), TopicUploadOptions(watermark = "cn"))
    assertTrue(result.isSuccess)
    assertEquals("quote", requests[0].query["action"])
    assertEquals("25", requests[0].query["pid"])
    assertEquals("10", requests[0].query["tid"])
    val multipart = requests.last().binaryBody!!.toString(Charsets.UTF_8)
    assertTrue(multipart.contains("reply-ticket"))
    assertTrue(multipart.contains("321"))
    assertTrue(multipart.contains("cn"))
    assertEquals("id", result.getOrThrow().id)
  }
  @Test fun richReplyEncodesTargetOptionsAttachmentsAndMentions() = runTest {
    val transport = Transport()
    val repo = DefaultNgaReadOnlyRepository(transport)
    val attachment = TopicAttachment("图", "id&1", "check+2", "https://img.nga.cn/a.png", true)
    val target = ReplyTarget("10", "25", mode = ReplyMode.Quote)
    assertTrue(repo.submitRichReply(session, target, "回复标题", "中文 & + [@用户甲]", listOf(attachment), TopicPostOptions(anonymous = true, hidden = true)).isSuccess)
    val body = transport.requests.single().bodyMap
    assertEquals("quote", body["action"])
    assertEquals("25", body["pid"])
    assertEquals("10", body["tid"])
    assertEquals("1", body["anony"])
    assertEquals("1", body["hidden"])
    assertEquals("回复标题", NgaEncoding.urlDecodeGbk(body.getValue("post_subject")))
    assertEquals("中文 & + [@用户甲]", NgaEncoding.urlDecodeGbk(body.getValue("post_content")))
    assertEquals("\tid&1", NgaEncoding.urlDecodeGbk(body.getValue("attachments")))
    assertEquals("\tcheck+2", NgaEncoding.urlDecodeGbk(body.getValue("attachments_check")))
    assertEquals("用户甲", NgaEncoding.urlDecodeGbk(body.getValue("mention")))
    assertFalse(body.containsKey("newvote"))
  }

  @Test fun commentPreparationAndSubmissionUseSameTargetAndMode() = runTest {
    val target = ReplyTarget("10", "0", mode = ReplyMode.Comment)
    val info = NgaApi().replyInfo(target)
    assertEquals("reply", info.query["action"])
    assertEquals("1", info.query["comment"])
    assertEquals("0", info.query["pid"])
    val transport = Transport()
    val repo = DefaultNgaReadOnlyRepository(transport)
    assertTrue(repo.submitRichReply(session, target, "", "贴条评论", emptyList(), TopicPostOptions()).isSuccess)
    val body = transport.requests.single().bodyMap
    assertEquals("reply", body["action"])
    assertEquals("1", body["comment"])
    assertFalse(body.containsKey("post_subject"))
    assertFalse(body.containsKey("attachments"))
    assertTrue(repo.submitRichReply(null, target, "", "内容测试", emptyList(), TopicPostOptions()).isFailure)
    assertTrue(repo.submitRichReply(session, target.copy(tid = "bad"), "", "内容测试", emptyList(), TopicPostOptions()).isFailure)
    assertTrue(repo.submitRichReply(session, target, "", "  ", emptyList(), TopicPostOptions()).isFailure)
    assertEquals(1, transport.requests.size)
  }
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

  @Test fun acceptsStructuredAndArraySuccessButNotAnErrorStatus() = runTest {
    val transport = Transport()
    val repo = DefaultNgaReadOnlyRepository(transport)
    listOf(
      """window.script_muti_get_var_store={"data":{"__MESSAGE":{"0":"提示","1":"<br/>发贴完毕","3":200}}};""",
      """{"data":["回复成功"]}""",
    ).forEach {
      transport.response = it
      assertTrue(repo.submitReply(session, "10", "0", "内容").isSuccess)
    }
    transport.response = """{"data":{"__MESSAGE":{"1":"主题已锁定","3":403}}}"""
    assertEquals("主题已锁定", repo.submitReply(session, "10", "0", "内容").exceptionOrNull()?.message)
    transport.response = """{"data":{"__MESSAGE":{"1":"发贴完毕","3":500}}}"""
    assertTrue(repo.submitReply(session, "10", "0", "内容").isFailure)
  }
}
