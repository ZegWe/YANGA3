package com.yanga.client.api

import org.junit.Assert.*
import org.junit.Test

class TopicPostOptionsTest {
  @Test fun flagsAndPollUseOfficialWireNames() {
    val options = TopicPostOptions(anonymous = true, hidden = true, selfReply = true, replyOnce = true,
      voteType = TopicVoteType.Poll, voteItems = "选项甲\n选项乙", voteHours = "24", voteVisibility = 2, voteReputation = "-100")
    assertNull(options.validationError())
    val body = NgaApi().newTopic(-7, "[讨论]标题", "测试正文", emptyList(), options).bodyMap
    assertEquals("1", body["anony"])
    assertEquals("1", body["hidden"])
    assertEquals("1", body["self_reply"])
    assertEquals("1073741824", body["tpic_misc_bit1"])
    assertEquals("0", body["newvote_type"])
    assertEquals("24", body["newvote_end"])
    assertEquals("4096", body["post_opt"])
    assertEquals("选项甲\n选项乙", NgaEncoding.urlDecodeGbk(body.getValue("newvote")))
  }

  @Test fun invalidPollsAreBlockedBeforePosting() {
    val poll = TopicPostOptions(voteType = TopicVoteType.Poll, voteItems = "甲\n乙")
    assertNull(poll.validationError())
    assertNotNull(poll.copy(voteMax = "3").validationError())
    assertNotNull(poll.copy(voteHours = "0").validationError())
    assertNotNull(poll.copy(voteVisibility = 1).validationError())
    assertNotNull(poll.copy(voteItems = "甲").validationError())
    assertNotNull(poll.copy(voteReputation = "21001").validationError())
    assertNull(poll.copy(voteItems = "===组一\n甲\n乙\n===组二\n丙\n丁", voteMax = "0").validationError())
    assertNotNull(poll.copy(voteType = TopicVoteType.Score).validationError())
    assertNull(poll.copy(voteType = TopicVoteType.Score).validationError(moderator = true))
  }

  @Test fun disabledPollDoesNotLeakOldPollFields() {
    assertTrue(TopicPostOptions(voteItems = "甲\n乙", voteHours = "24", voteVisibility = 2).fields().isEmpty())
  }

  @Test fun mentionsAreDeduplicatedAndEncodedSeparatelyFromBody() {
    val request = NgaApi().newTopic(7, "标题", "[@张三][@张三][@李四]", emptyList())
    assertEquals("张三\t李四", NgaEncoding.urlDecodeGbk(request.bodyMap.getValue("mention")))
  }

  @Test fun preparationReadsPermissionsAndBothCategoryShapes() {
    val info = TopicPostPreparationParser.parse("""{"data":{"__F":{"bit_data":256},"if_moderator":2,"subject":"[讨论]","warning":"<b>版规</b>"}}""")
    assertTrue(info.categoryRequired)
    assertTrue(info.moderator)
    assertEquals("[讨论]", info.defaultSubject)
    assertEquals("版规", info.warning)
    for (raw in listOf(
      """{"data":{"0":{"0":{"0":"讨论","1":0},"1":{"0":"新闻","1":1}}}}""",
      """{"data":[["讨论",0],["新闻",1]]}""",
    )) assertEquals(listOf("讨论", "新闻"), TopicPostPreparationParser.categories(raw))
  }

  @Test fun compressionAndWatermarkReachMultipartBody() {
    val request = NgaTopicPosting.uploadRequest(NgaApi(), 7, "ticket", "图.png", "image/png", byteArrayOf(1), TopicUploadOptions("8", "cn", "说明"))
    val text = request.binaryBody!!.toString(Charsets.UTF_8)
    assertTrue(text.contains("name=\"attachment_file1_auto_size\"\r\n\r\n8"))
    assertTrue(text.contains("name=\"attachment_file1_watermark\"\r\n\r\ncn"))
    assertTrue(text.contains("name=\"attachment_file1_dscp\"\r\n\r\n说明"))
  }
}
