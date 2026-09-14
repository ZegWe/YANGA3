package com.yanga.client.api
import org.junit.Assert.*
import org.junit.Test
class NgaPersonalTopicParserTest {
  @Test fun keepsReplyTargetsInBothListShapes() {
    val row = """{"tid":123,"pid":456,"subject":"标题","content":"回复"}"""
    for (list in listOf("[$row]", "{\"0\":$row}")) {
      val page = NgaPersonalTopicParser.parse("{\"data\":{\"__T\":$list}}")
      assertEquals("456", page.items.single().pid)
      assertTrue(page.hasNextPage)
    }
  }
  @Test fun emptyPageEndsPagination() {
    assertFalse(NgaPersonalTopicParser.parse("""{"data":{"__T":[]}}""").hasNextPage)
  }
  @Test(expected = NgaApiException::class) fun loginFailureIsNotAnEmptyList() {
    NgaPersonalTopicParser.parse("""{"error":"请登录"}""")
  }
}
