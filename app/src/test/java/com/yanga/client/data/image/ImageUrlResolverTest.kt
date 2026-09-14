package com.yanga.client.data.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageUrlResolverTest {
  @Test fun requestsMigrateOldAttachmentHostsAndRelativePaths() {
    val path = "mon_202307/10/-5i9b7Q2s-7ptqXpZ6zT3cSb2-7u.gif"
    for (raw in listOf("./$path", "/$path", path, "https://img.nga.178.com/attachments/$path", "http://img6.nga.178.com/attachments/$path")) {
      assertEquals("https://img.nga.cn/attachments/$path", ImageUrlResolver.resolveForRequest(raw))
    }
    assertEquals("https://example.com/image.gif", ImageUrlResolver.resolveForRequest("https://example.com/image.gif"))
    assertEquals("https://img.nga.cn/attachments/a.png?v=2#original", ImageUrlResolver.resolveForRequest("https://img.nga.178.com/attachments/a.png?v=2#original"))
  }
  @Test fun requestsExpandUserFileShards() {
    assertEquals("https://user-file.nga.cn/01/00/00/1Qabc.png", ImageUrlResolver.resolveForRequest(".u/1Qabc.png"))
  }

  @Test
  fun resolveMigratesLegacyEmoticons() {
    for (scheme in listOf("https:", "http:", "")) {
      assertEquals(
        "https://img4.nga.cn/ngabbs/post/smile/ac21.png?v=2",
        ImageUrlResolver.resolve("$scheme//img4.nga.178.com/ngabbs/post/smile/ac21.png?v=2"),
      )
    }
    val current = "https://img4.nga.cn/ngabbs/post/smile/ac21.png"
    assertEquals(current, ImageUrlResolver.resolve(current))
  }

  @Test
  fun resolveExpandsRelativeNgaAttachmentPath() {
    assertEquals(
      "https://img.nga.178.com/attachments/mon_a.jpg",
      ImageUrlResolver.resolve("./mon_a.jpg"),
    )
  }

  @Test
  fun resolveMigratesPersistedBoardIconUrls() {
    assertEquals(
      "https://img4.nga.cn/ngabbs/nga_classic/f/app/7.png",
      ImageUrlResolver.resolve("https://img4.nga.178.com/ngabbs/nga_classic/f/app/7.png"),
    )
    for (scheme in listOf("https:", "http:", "")) {
      assertEquals(
        "https://img4.nga.cn/proxy/cache_attach/ficon/123v.png?v=2",
        ImageUrlResolver.resolve("$scheme//img4.nga.178.com/proxy/cache_attach/ficon/123v.png?v=2"),
      )
    }
    assertEquals(
      "https://img4.nga.178.com/avatars/avatar.png",
      ImageUrlResolver.resolve("https://img4.nga.178.com/avatars/avatar.png"),
    )
  }

  @Test
  fun resolveNormalizesLegacyImg6AttachmentUrls() {
    assertEquals(
      "https://img.nga.178.com/attachments/mon_a.jpg",
      ImageUrlResolver.resolve("http://img6.nga.178.com/attachments/mon_a.jpg"),
    )
    assertEquals(
      "https://img.nga.178.com/attachments/mon_a.jpg",
      ImageUrlResolver.resolve("https://img6.nga.178.com/attachments/mon_a.jpg"),
    )
  }

  @Test
  fun resolveKeepsOtherAbsoluteHttpUrls() {
    assertEquals(
      "http://example.com/a.png",
      ImageUrlResolver.resolve("http://example.com/a.png"),
    )
  }

  @Test
  fun resolveNormalizesProtocolRelativeImg6Attachments() {
    assertEquals(
      "https://img.nga.178.com/attachments/mon_a.jpg",
      ImageUrlResolver.resolve("//img6.nga.178.com/attachments/mon_a.jpg"),
    )
    assertEquals(
      "https://img.example.com/a.png",
      ImageUrlResolver.resolve("//img.example.com/a.png"),
    )
  }

  @Test
  fun resolveAddsHttpsSchemeForProtocolRelativeUrls() {
    assertEquals(
      "https://img.example.com/a.png",
      ImageUrlResolver.resolve("//img.example.com/a.png"),
    )
  }

  @Test
  fun fileNameUsesLastPathSegment() {
    assertEquals(
      "mon_a.jpg",
      ImageUrlResolver.fileName("https://img.nga.178.com/attachments/mon_a.jpg"),
    )
  }

  @Test
  fun fileNameStripsQueryParameters() {
    assertTrue(ImageUrlResolver.fileName("https://example.com/a.png?v=1").startsWith("a.png"))
  }
}
