package com.yanga.client.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AttachmentFileTypeTest {
  @Test
  fun categoryUsesFileNameExtension() {
    assertEquals(AttachmentFileCategory.Audio, AttachmentFileType.category("8xQ6-kcb3Kf.mp3"))
    assertEquals(AttachmentFileCategory.Image, AttachmentFileType.category("photo.webp"))
    assertEquals(AttachmentFileCategory.Archive, AttachmentFileType.category("bundle.zip"))
  }

  @Test
  fun categoryFallsBackToUrlExtension() {
    assertEquals(
      AttachmentFileCategory.Image,
      AttachmentFileType.category(name = "attachment", url = "https://img.nga.178.com/attachments/mon_a.png"),
    )
  }

  @Test
  fun categoryReturnsOtherForUnknownExtension() {
    assertEquals(AttachmentFileCategory.Other, AttachmentFileType.category("readme"))
  }
}
