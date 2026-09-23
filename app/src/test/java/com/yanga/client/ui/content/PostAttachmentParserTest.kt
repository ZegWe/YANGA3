package com.yanga.client.ui.content

import org.junit.Assert.*
import org.junit.Test

class PostAttachmentParserTest {
  @Test fun attachmentsAreFilesAndNeverEnterImageGallery() {
    for (extension in listOf("zip", "7z", "pdf", "ps1", "unknown")) {
      val path = "./mon_202609/file.$extension"
      val expected = PostContentPart.Attachment(
        "https://img.nga.178.com/attachments/mon_202609/file.$extension", "file.$extension",
      )
      for (body in listOf("[attach]$path[/attach]", "[ATTACH]$path[/ATTACH]", path)) {
        val parts = PostContentParser.parse("before $body after")
        assertEquals(listOf(PostContentPart.Text("before"), expected, PostContentPart.Text("after")), parts)
        assertTrue(PostContentParser.collectImageUrls(parts).isEmpty())
        assertEquals(setOf(expected.url), PostContentParser.collectAttachmentUrls(parts))
      }
    }
  }

  @Test fun mediaAttachmentsKeepTheirActualTypes() {
    assertTrue(PostContentParser.parse("[attach]./mon_a.PNG?download=1[/attach]").single() is PostContentPart.Image)
    assertTrue(PostContentParser.parse("[attach]./mon_a.mp3[/attach]").single() is PostContentPart.Audio)
    assertTrue(PostContentParser.parse("[attach]./mon_a.mp4[/attach]").single() is PostContentPart.Video)
    val quote = PostContentParser.parse("[quote][attach]https://img.nga.cn/attachments/a.zip[/attach][/quote]").single() as PostContentPart.Quote
    assertEquals(PostContentPart.Attachment("https://img.nga.cn/attachments/a.zip", "a.zip"), quote.parts.single())
  }

  @Test fun malformedAndUnsafeAttachmentsRemainText() {
    for (body in listOf("[attach]./mon_a.zip", "[attach]javascript:alert(1)[/attach]", "[attach]123[/attach]")) {
      assertEquals(listOf(PostContentPart.Text(body)), PostContentParser.parse(body))
    }
  }
}
