package com.yanga.client.api

import org.junit.Assert.assertEquals
import org.junit.Test

class NgaCheckInParserTest {
  @Test fun readsServerDaysFromActualStatusShape() {
    assertEquals(true, NgaCheckInParser.parseStatus("""{"data":{"0":{"continued":2,"sum":926,"last_time":1790098569,"last_day":20719,"now_day":20719},"1":{"money":0}},"time":1790098955}"""))
    assertEquals(false, NgaCheckInParser.parseStatus("""{"data":{"0":{"continued":2,"sum":926,"last_day":20718,"now_day":20719}}}"""))
    assertEquals(false, NgaCheckInParser.parseStatus("""{"data":[{"last_day":0,"now_day":20719}]}"""))
    assertEquals(true, NgaCheckInParser.parseStatus("""{"data":[{"last_day":"20719","now_day":"20719"}]}"""))
  }

  @Test fun unknownAndErrorStatusNeverBecomeNotCheckedIn() {
    for (raw in listOf(
      "<html>login</html>",
      """{"data":{"0":{"sum":926}}}""",
      """{"data":{"0":{"last_day":20719}}}""",
      """{"data":{"0":{"last_day":"bad","now_day":20719}}}""",
      """{"data":{"0":{"last_day":20720,"now_day":20719}}}""",
      """{"error":{"0":"请登录"},"data":{"0":{"last_day":20719,"now_day":20719}}}""",
    )) {
      org.junit.Assert.assertThrows(NgaApiException::class.java) { NgaCheckInParser.parseStatus(raw) }
    }
  }

  @Test fun acceptsActualDuplicateCheckInErrorEnvelope() {
    val message = "你今天已经签到了(以当前服务器时区 2026-09-14 16:59:22 +0800 计算)"
    assertEquals(message, NgaCheckInParser.parse("""{"error":{"0":"$message"}}"""))
    assertEquals(message, NgaCheckInParser.parse("""{"error":["$message"]}"""))
  }
  @Test(expected = NgaApiException::class) fun genuineErrorOverridesSuccessText() {
    NgaCheckInParser.parse("""{"error":{"0":"请登录"},"data":{"0":"签到成功"}}""")
  }
  @Test(expected = NgaApiException::class) fun doesNotAcceptNegatedSuccess() {
    NgaCheckInParser.parse("""{"data":{"0":"未签到成功，请重试"}}""")
  }
  @Test fun acceptsSuccessAndAlreadyCheckedIn() {
    for (message in listOf("签到成功", "今天已经签到")) {
      assertEquals(message, NgaCheckInParser.parse("""{"data":{"0":"$message"}}"""))
    }
  }
  @Test(expected = NgaApiException::class) fun rejectsHtml() {
    NgaCheckInParser.parse("<html>login</html>")
  }
  @Test(expected = NgaApiException::class) fun rejectsFailure() {
    NgaCheckInParser.parse("""{"error":{"0":"请登录"}}""")
  }
}
