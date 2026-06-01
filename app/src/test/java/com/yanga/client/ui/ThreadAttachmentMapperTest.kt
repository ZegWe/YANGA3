package com.yanga.client.ui

import com.yanga.client.api.NgaThreadAttachment
import com.yanga.client.api.NgaThreadPost
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadAttachmentMapperTest {
  @Test
  fun threadPostPreviewKeepsAttachmentMetadata() {
    val post =
      NgaThreadPost(
        pid = "101",
        tid = "46634352",
        fid = "7",
        authorId = "42",
        author = "reader",
        subject = "",
        content = "正文",
        lou = 0,
        postDate = 1770000000L,
        attachments =
          listOf(
            NgaThreadAttachment(
              name = "sample image.png",
              url = "https://img.nga.178.com/attachments/mon_202606/01/sample.png",
            ),
          ),
      )

    val preview = post.toPreview()

    assertEquals(
      listOf(
        PostAttachmentPreview(
          name = "sample image.png",
          url = "https://img.nga.178.com/attachments/mon_202606/01/sample.png",
        ),
      ),
      preview.attachments,
    )
  }
}
