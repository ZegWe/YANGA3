package com.yanga.client.ui

import com.yanga.client.ui.content.PostContentPart
import com.yanga.client.ui.content.PostTextStyleRange

internal sealed class PostContentBlock {
  data class Inline(val items: List<PostInlineItem>) : PostContentBlock()

  data class Quote(val part: PostContentPart.Quote) : PostContentBlock()

  data class Image(val part: PostContentPart.Image) : PostContentBlock()

  data class Audio(val part: PostContentPart.Audio) : PostContentBlock()
  data class Video(val part: PostContentPart.Video) : PostContentBlock()
  data class Structured(val part: PostContentPart) : PostContentBlock()
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
      is PostContentPart.Audio -> {
        flushInlineItems()
        blocks += PostContentBlock.Audio(part)
      }
      is PostContentPart.Video -> {
        flushInlineItems()
        blocks += PostContentBlock.Video(part)
      }
      is PostContentPart.Attachment, is PostContentPart.ListBlock, is PostContentPart.Collapse, is PostContentPart.Code,
      is PostContentPart.Heading, is PostContentPart.Table, PostContentPart.Rule -> {
        flushInlineItems()
        blocks += PostContentBlock.Structured(part)
      }
    }
  }

  flushInlineItems()
  return blocks
}
