package com.yanga.client.api

import org.junit.Assert.*
import org.junit.Test

class NgaUserProfileParserTest {
  @Test fun parsesObjectAndArrayWrappers() {
    val user = """{"uid":42,"username":"用户","sign":"签名","postnum":12}"""
    for (raw in listOf("{\"data\":{\"0\":$user}}", "{\"data\":[$user]}")) {
      val parsed = NgaUserProfileParser.parse(raw)
      assertEquals("42", parsed.uid)
      assertEquals(12, parsed.postCount)
      assertEquals("签名", parsed.signature)
      assertNull(parsed.registeredAt)
    }
  }
  @Test(expected = NgaApiException::class) fun rejectsMissingUser() {
    NgaUserProfileParser.parse("""{"data":{}}""")
  }
}
