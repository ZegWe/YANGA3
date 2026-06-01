package com.yanga.client.data.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageUrlResolverTest {
  @Test
  fun resolveExpandsRelativeNgaAttachmentPath() {
    assertEquals(
      "https://img.nga.178.com/attachments/mon_a.jpg",
      ImageUrlResolver.resolve("./mon_a.jpg"),
    )
  }

  @Test
  fun resolveKeepsAbsoluteHttpsUrls() {
    assertEquals(
      "https://img4.nga.178.com/ngabbs/nga_classic/f/app/7.png",
      ImageUrlResolver.resolve("https://img4.nga.178.com/ngabbs/nga_classic/f/app/7.png"),
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
