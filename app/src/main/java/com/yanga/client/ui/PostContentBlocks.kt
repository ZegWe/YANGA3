package com.yanga.client.ui

import com.yanga.client.ui.content.PostContentPart
import com.yanga.client.ui.content.PostTextStyleRange

internal sealed class PostContentBlock {
  data class Inline(val items: List<PostInlineItem>) : PostContentBlock()

  data class Quote(val part: PostContentPart.Quote) : PostContentBlock()

  data class Image(val part: PostContentPart.Image) : PostContentBlock()
}

internal sealed class PostInlineItem {
  data class Text(
    val text: String,
    val styles: List<PostTextStyleRange>,
  ) : PostInlineItem()

  data class Emoticon(val part: PostContentPart.Emoticon) : PostInlineItem()
}

internal fun groupPostContentParts(parts: List<PostContentPart>): List<PostContentBlock> {
  val blocks = mutableListOf<PostContentBlock>()
  val inlineItems = mutableListOf<PostInlineItem>()

  fun flushInlineItems() {
    if (inlineItems.isNotEmpty()) {
      blocks += PostContentBlock.Inline(inlineItems.toList())
      inlineItems.clear()
    }
  }

  for (part in parts) {
    when (part) {
      is PostContentPart.Text -> {
        if (part.text.isNotEmpty()) {
          inlineItems += PostInlineItem.Text(part.text, part.styles)
        }
      }
      is PostContentPart.Emoticon -> {
        inlineItems += PostInlineItem.Emoticon(part)
      }
      is PostContentPart.Quote -> {
        flushInlineItems()
        blocks += PostContentBlock.Quote(part)
      }
      is PostContentPart.Image -> {
        flushInlineItems()
        blocks += PostContentBlock.Image(part)
      }
    }
  }

  flushInlineItems()
  return blocks
}
