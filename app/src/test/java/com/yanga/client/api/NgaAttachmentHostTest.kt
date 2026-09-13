package com.yanga.client.api

import com.yanga.client.data.image.ImageUrlResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class NgaAttachmentHostTest {
  @Test
  fun serverAttachmentBaseReachesPostsCommentsHotRepliesAndAttachments() {
    val raw = """{"data":{
      "__GLOBAL":{"_ATTACH_BASE_VIEW":"img.nga.cn/attachments"},
      "__T":{"tid":47544549},
      "__R":{"0":{"pid":1,"lou":0,"content":"[img]./mon_a.jpg[/img]",
        "comment":{"0":{"pid":2,"content":"[img]/mon_b.jpg[/img]"}},
        "hotreply":{"0":{"pid":3,"content":"[img]mon_c.jpg[/img]"}},
        "attachs":{"0":{"attachurl":"./mon_d.jpg"}}
      }}
    }}"""
    val post = NgaThreadParser.parseRead(raw).posts.single()
    assertEquals("[img]https://img.nga.cn/attachments/mon_a.jpg[/img]", post.content)
    assertEquals("[img]https://img.nga.cn/attachments/mon_b.jpg[/img]", post.embeddedComments.single().content)
    assertEquals("[img]https://img.nga.cn/attachments/mon_c.jpg[/img]", post.hotReplies.single().content)
    assertEquals("https://img.nga.cn/attachments/mon_d.jpg", post.attachments.single().url)
  }

  @Test
  fun attachmentBaseSupportsSchemesAndPreservesExternalImages() {
    for (base in listOf("img.nga.cn/attachments", "//img.nga.cn/attachments/", "https://img.nga.cn/attachments/")) {
      assertEquals("https://img.nga.cn/attachments/mon_a.jpg", ImageUrlResolver.resolve("./mon_a.jpg", base))
      assertEquals("https://example.com/a.jpg", ImageUrlResolver.resolve("https://example.com/a.jpg", base))
    }
    assertEquals(ImageUrlResolver.resolve("./mon_a.jpg"), ImageUrlResolver.resolve("./mon_a.jpg", null))
  }
}
