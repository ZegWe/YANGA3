package com.yanga.client.api

import org.junit.Assert.assertEquals
import org.junit.Test

class NgaCheckInParserTest {
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
