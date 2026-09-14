package com.yanga.client.api

import org.junit.Assert.assertEquals
import org.junit.Test

class NgaCheckInParserTest {
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
