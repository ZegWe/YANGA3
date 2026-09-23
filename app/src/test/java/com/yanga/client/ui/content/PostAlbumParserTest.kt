package com.yanga.client.ui.content

import org.junit.Assert.*
import org.junit.Test

class PostAlbumParserTest {
  @Test fun generatedAlbumKeepsTitleAndMakesAllImagesAvailableToPreview() {
    val source = ComposerMarkup.build("album", listOf("旅行照片", "https://img.nga.cn/attachments/mon_a.jpg\nhttps://img.nga.cn/attachments/mon_b.png"), "")
    val album = PostContentParser.parse("开头\n$source\n结尾").filterIsInstance<PostContentPart.Album>().single()
    assertEquals("旅行照片", album.title)
    assertEquals(listOf("https://img.nga.cn/attachments/mon_a.jpg", "https://img.nga.cn/attachments/mon_b.png"), album.images.map { it.url })
    assertEquals(album.images.map { it.url }, PostContentParser.collectImageUrls(listOf(album)))
  }

  @Test fun albumSupportsRelativeImagesAndLeavesMalformedContentVisible() {
    val album = PostContentParser.parse("[ALBUM=旧图]\n./mon_a.jpg\n[img]./mon_b.png[/img]\n[/ALBUM]").single() as PostContentPart.Album
    assertEquals(2, album.images.size)
    assertTrue(album.images.all { it.url.startsWith("https://") })
    assertTrue(PostContentParser.parse("[album=坏格式]无效图片[/album]").single() is PostContentPart.Text)
    assertTrue(PostContentParser.parse("[album=未闭合]内容").any { it is PostContentPart.Text })
  }
}
