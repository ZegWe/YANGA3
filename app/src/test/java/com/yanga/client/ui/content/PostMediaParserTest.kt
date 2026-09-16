package com.yanga.client.ui.content

import org.junit.Assert.*
import org.junit.Test

class PostMediaParserTest {
  @Test fun officialMediaTagsPreserveVideoAndInferAudio() {
    for (tag in listOf("flash=video", "media", "video", "FLASH=VIDEO")) {
      val close = tag.substringBefore('=')
      val parts = PostContentParser.parse("before [$tag]./mon_clip.mp4?x=1&amp;y=2[/$close] after")
      assertEquals(PostContentPart.Text("before"), parts.first())
      val video = parts[1] as PostContentPart.Video
      assertEquals("https://img.nga.178.com/attachments/mon_clip.mp4?x=1&y=2", video.url)
      assertTrue(video.direct)
      assertEquals(PostContentPart.Text("after"), parts.last())
    }
    for (tag in listOf("media", "media=audio", "audio", "FLASH=AUDIO")) {
      val part = PostContentParser.parse("[$tag]//example.com/a.MP3[/${tag.substringBefore('=')}]").single()
      assertEquals(PostContentPart.Audio("https://example.com/a.MP3", "a.MP3"), part)
    }
  }

  @Test fun externalProvidersRemainVisibleAsLinks() {
    val url = "https://www.bilibili.com/video/BV1example?p=2"
    assertEquals(PostContentPart.Video(url, "BV1example", false), PostContentParser.parse("[flash]$url[/flash]").single())
  }

  @Test fun htmlMediaAndNestedMediaAreSupported() {
    val part = PostContentParser.parse("""<video controls><source src="https://example.com/a.mp4"></video>""").single() as PostContentPart.Video
    assertTrue(part.direct)
    val quote = PostContentParser.parse("[quote][collapse=声音][audio]https://example.com/a.ogg[/audio][/collapse][/quote]").single() as PostContentPart.Quote
    assertTrue((quote.parts.single() as PostContentPart.Collapse).parts.single() is PostContentPart.Audio)
    assertTrue(PostContentParser.parse("""<audio src='https://example.com/a.mp3'></audio>""").single() is PostContentPart.Audio)
  }

  @Test fun malformedOrUnsafeMediaIsNotSilentlyDiscardedOrMadePlayable() {
    for (body in listOf("[flash]javascript:alert(1)[/flash]", "[video]file:///private.mp4[/video]", "[flash=video]unfinished", "[media]not a url[/media]")) {
      val parts = PostContentParser.parse(body)
      assertTrue(parts.none { it is PostContentPart.Video || it is PostContentPart.Audio })
      assertTrue(parts.filterIsInstance<PostContentPart.Text>().joinToString("") { it.text }.isNotBlank())
    }
  }
}
