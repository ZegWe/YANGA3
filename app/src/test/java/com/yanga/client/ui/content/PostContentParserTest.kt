package com.yanga.client.ui.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostContentParserTest {
  @Test
  fun parseExtractsBbCodeImage() {
    val parts = PostContentParser.parse("hello [img]./mon_a.jpg[/img] world")

    assertEquals(3, parts.size)
    assertEquals(PostContentPart.Text("hello"), parts[0])
    assertEquals(
      PostContentPart.Image("http://img6.nga.178.com/attachments/mon_a.jpg"),
      parts[1],
    )
    assertEquals(PostContentPart.Text("world"), parts[2])
  }

  @Test
  fun parseExtractsQuoteAndImage() {
    val parts =
      PostContentParser.parse(
        "intro [quote]quoted[/quote] after [img]https://img.example.com/a.png[/img]",
      )

    assertTrue(parts[0] is PostContentPart.Text)
    assertEquals(PostContentPart.Quote("quoted"), parts[1])
    assertTrue(parts[2] is PostContentPart.Text)
    assertEquals(PostContentPart.Image("https://img.example.com/a.png"), parts[3])
  }

  @Test
  fun parseExtractsHtmlImageTag() {
    val parts = PostContentParser.parse("""before <img src="./mon_b.jpg"> after""")

    assertEquals(
      PostContentPart.Image("http://img6.nga.178.com/attachments/mon_b.jpg"),
      parts[1],
    )
  }

  @Test
  fun parseExtractsStandaloneRelativeImagePath() {
    val parts = PostContentParser.parse("see ./mon_c.jpg here")

    assertEquals(
      PostContentPart.Image("http://img6.nga.178.com/attachments/mon_c.jpg"),
      parts[1],
    )
  }
}
