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
      PostContentPart.Image("https://img.nga.178.com/attachments/mon_a.jpg"),
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
      PostContentPart.Image("https://img.nga.178.com/attachments/mon_b.jpg"),
      parts[1],
    )
  }

  @Test
  fun parseExtractsStandaloneRelativeImagePath() {
    val parts = PostContentParser.parse("see ./mon_c.jpg here")

    assertEquals(
      PostContentPart.Image("https://img.nga.178.com/attachments/mon_c.jpg"),
      parts[1],
    )
  }

  @Test
  fun parsePreservesInlineTextFormatting() {
    val parts =
      PostContentParser.parse(
        "plain [b]bold[/b] [i]italic[/i] [u]under[/u] [del]gone[/del]",
      )

    val text = parts.single() as PostContentPart.Text
    assertEquals("plain bold italic under gone", text.text)
    assertEquals(PostTextStyleRange(6, 10, bold = true), text.styles[0])
    assertEquals(PostTextStyleRange(11, 17, italic = true), text.styles[1])
    assertEquals(PostTextStyleRange(18, 23, underline = true), text.styles[2])
    assertEquals(PostTextStyleRange(24, 28, strikeThrough = true), text.styles[3])
  }

  @Test
  fun parsePreservesColorSizeAndLinkFormatting() {
    val parts =
      PostContentParser.parse(
        "[color=red]red[/color] [size=150%]big[/size] [url=https://example.com]site[/url]",
      )

    val text = parts.single() as PostContentPart.Text
    assertEquals("red big site", text.text)
    assertEquals(PostTextStyleRange(0, 3, color = "red"), text.styles[0])
    assertEquals(PostTextStyleRange(4, 7, sizePercent = 150), text.styles[1])
    assertEquals(PostTextStyleRange(8, 12, linkUrl = "https://example.com"), text.styles[2])
  }

  @Test
  fun parseExtractsOfficialEmoticon() {
    val parts = PostContentParser.parse("hi [s:ac:囧] there")

    assertEquals(PostContentPart.Text("hi"), parts[0])
    assertEquals(
      PostContentPart.Emoticon(
        code = "[s:ac:囧]",
        url = "https://img4.nga.178.com/ngabbs/post/smile/ac21.png",
        alt = "囧",
      ),
      parts[1],
    )
    assertEquals(PostContentPart.Text("there"), parts[2])
  }

  @Test
  fun parseCleansQuoteMetadataAndKeepsFormatting() {
    val parts =
      PostContentParser.parse(
        "[quote][pid=253176649,12937812,2]Reply[/pid] [b]Post by [uid=42]reader[/uid] (2026-06-01):[/b]<br/>quoted[/quote]",
      )

    val quote = parts.single() as PostContentPart.Quote
    assertEquals("Reply Post by reader (2026-06-01):\nquoted", quote.text)
    assertTrue(quote.styles.any { it.bold })
  }

  @Test
  fun parseNormalizesImageThumbnailSuffix() {
    val parts = PostContentParser.parse("[img]https://img.example.com/a.gif.thumb.jpg[/img]")

    assertEquals(PostContentPart.Image("https://img.example.com/a.gif"), parts.single())
  }

  @Test
  fun parseStripsMalformedAlignTags() {
    val parts = PostContentParser.parse("{align=center]centered[/align]")

    assertEquals(PostContentPart.Text("centered"), parts.single())
  }

  @Test
  fun parsePreservesBbCodeLinkTarget() {
    val parts = PostContentParser.parse("see [url=https://bbs.nga.cn/read.php?tid=6406100]thread[/url]")

    assertEquals(1, parts.size)
    val text = parts.single() as PostContentPart.Text
    assertEquals("see thread", text.text)
    assertEquals(
      PostTextStyleRange(
        start = 4,
        end = 10,
        linkUrl = "https://bbs.nga.cn/read.php?tid=6406100",
      ),
      text.styles.single(),
    )
  }

  @Test
  fun parsePreservesStandaloneUrlAsLink() {
    val parts = PostContentParser.parse("open https://example.com/a?b=1 now")

    assertEquals(1, parts.size)
    val text = parts.single() as PostContentPart.Text
    assertEquals("open https://example.com/a?b=1 now", text.text)
    assertEquals(
      PostTextStyleRange(
        start = 5,
        end = 30,
        linkUrl = "https://example.com/a?b=1",
      ),
      text.styles.single(),
    )
  }
}
