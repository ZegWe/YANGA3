package com.yanga.client.ui

import com.yanga.client.ui.content.PostContentPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostContentBlocksTest {
  @Test
  fun groupPostContentPartsKeepsTextAndEmoticonInSameInlineBlock() {
    val emoticon =
      PostContentPart.Emoticon(
        code = "[s:ac:囧]",
        url = "https://img4.nga.178.com/ngabbs/post/smile/ac21.png",
        alt = "囧",
      )

    val blocks =
      groupPostContentParts(
        listOf(
          PostContentPart.Text("plain"),
          emoticon,
          PostContentPart.Text("after"),
          PostContentPart.Image("https://img.example.com/a.png"),
        ),
      )

    assertEquals(2, blocks.size)
    val inline = blocks[0]
    assertTrue(inline is PostContentBlock.Inline)
    inline as PostContentBlock.Inline
    assertEquals(
      listOf(
        PostInlineItem.Text("plain", emptyList()),
        PostInlineItem.Emoticon(emoticon),
        PostInlineItem.Text("after", emptyList()),
      ),
      inline.items,
    )
    assertEquals(
      PostContentBlock.Image(PostContentPart.Image("https://img.example.com/a.png")),
      blocks[1],
    )
  }
}
