package com.yanga.client.api

import org.junit.Assert.*
import org.junit.Test

class NgaTopicPostingTest {
  @Test fun newTopicEncodesTextAndPairsAttachments() {
    val attachments = listOf(TopicAttachment("图.png", "a&1", "c=1", "https://img.nga.178.com/a.png", true),
      TopicAttachment("图2.png", "a2", "c2", "https://img.nga.178.com/b.png", true))
    val request = NgaApi().newTopic(321, "标题 & 测试", "正文\n测试", attachments)
    assertEquals("new", request.bodyMap["action"])
    assertEquals("2", request.bodyMap["step"])
    assertEquals("321", request.bodyMap["fid"])
    assertEquals(NgaEncoding.urlEncodeGbk("标题 & 测试"), request.bodyMap["post_subject"])
    assertEquals("%09a%261%09a2", request.bodyMap["attachments"])
    assertEquals("%09c%3D1%09c2", request.bodyMap["attachments_check"])
    assertEquals("8", request.query["__output"])
  }

  @Test fun multipartIncludesFileBytesAndRequiredFields() {
    val request = NgaTopicPosting.uploadRequest(NgaApi(), 321, "ticket", "示例.png", "image/png", byteArrayOf(0, 1, 2, 3))
    val body = request.binaryBody!!
    val text = body.toString(Charsets.UTF_8)
    assertTrue(request.headers["Content-Type"]!!.contains("boundary=Yanga"))
    assertTrue(text.contains("name=\"attachment_file1\"; filename=\"示例.png\""))
    assertTrue(text.contains("name=\"auth\"\r\n\r\nticket"))
    assertTrue(text.contains("name=\"origin_domain\"\r\n\r\nbbs.nga.cn"))
    assertTrue(body.toList().windowed(4).any { it == listOf<Byte>(0, 1, 2, 3) })
  }

  @Test fun parsesWrappedUploadAndRelativeImageUrl() {
    val result = NgaTopicPosting.uploaded("window.script_muti_get_var_store={\"data\":{\"attachments\":\"a\",\"attachments_check\":\"b\",\"url\":\"./mon_202609/1.png\"}}", "图.png", true)
    assertEquals("[img]https://img.nga.cn/attachments/mon_202609/1.png[/img]", result.markup)
  }

  @Test fun acceptsBareUploadPathsReturnedByAttachPhp() {
    for (path in listOf("mon_202609/321/test.png", "./mon_202609/321/test.png", "/mon_202609/321/test.png")) {
      val result = NgaTopicPosting.uploaded(
        """window.script_muti_get_var_store={"data":{"attachments":"uploaded-id","attachments_check":"uploaded-check","url":"$path"}};""",
        "测试.png", true,
      )
      assertEquals("https://img.nga.cn/attachments/mon_202609/321/test.png", result.url)
      val request = NgaApi().newTopic(321, "标题", result.markup, listOf(result))
      assertEquals("%09uploaded-id", request.bodyMap["attachments"])
      assertEquals("%09uploaded-check", request.bodyMap["attachments_check"])
    }
  }

  @Test fun preservesAbsoluteAndProtocolRelativeUploadUrls() {
    for (path in listOf("https://img8.nga.cn/attachments/test.png", "http://img8.nga.cn/attachments/test.png", "//img8.nga.cn/attachments/test.png")) {
      val result = NgaTopicPosting.uploaded(
        """{"data":{"attachments":"a","attachments_check":"b","url":"$path"}}""", "test.png", true,
      )
      assertEquals("https://img8.nga.cn/attachments/test.png", result.url)
    }
  }

  @Test fun topLevelUploadErrorIsNotReportedAsIncompleteResult() {
    val failure = runCatching {
      NgaTopicPosting.uploaded("""{"error_code":9}""", "test.png", true)
    }.exceptionOrNull()
    assertEquals("附件过大，请压缩后重试", failure?.message)
  }

  @Test fun permissionErrorDoesNotBecomeAnUploadTicket() {
    val error = runCatching { NgaTopicPosting.auth("{\"error\":{\"0\":\"帐号声望不足\"}}") }.exceptionOrNull()
    assertEquals("帐号声望不足", error?.message)
  }

  @Test fun incompleteUploadCannotBePublished() {
    assertTrue(runCatching { NgaTopicPosting.uploaded("{\"data\":{\"url\":\"https://img.nga.cn/a.png\"}}", "a.png", true) }.isFailure)
  }
}
