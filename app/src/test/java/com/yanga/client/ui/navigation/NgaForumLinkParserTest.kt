package com.yanga.client.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NgaForumLinkParserTest {
  @Test
  fun parseThreadReadUrlExtractsTidAndPage() {
    val destination =
      NgaForumLinkParser.threadDestination("https://bbs.nga.cn/read.php?tid=6406100&page=3")

    assertEquals(ForumThreadDestination(tid = "6406100", page = 3), destination)
  }

  @Test
  fun parseThreadReadUrlAcceptsNga178Domain() {
    val destination = NgaForumLinkParser.threadDestination("https://nga.178.com/read.php?tid=25968165")

    assertEquals(ForumThreadDestination(tid = "25968165"), destination)
  }

  @Test
  fun parseNgaThreadSchemeExtractsThreadId() {
    val destination = NgaForumLinkParser.threadDestination("nga://thread/12937812")

    assertEquals(ForumThreadDestination(tid = "12937812"), destination)
  }

  @Test
  fun parseNonForumUrlReturnsNull() {
    assertNull(NgaForumLinkParser.threadDestination("https://example.com/read.php?tid=1"))
  }
}
