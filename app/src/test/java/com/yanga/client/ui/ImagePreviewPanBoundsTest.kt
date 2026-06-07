package com.yanga.client.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class ImagePreviewPanBoundsTest {
  @Test
  fun postPreviewImageUrlsOnlyIncludesTheCurrentPostBody() {
    val post =
      PostPreview(
        author = "reader",
        floor = "1楼",
        time = "now",
        avatarInitial = "R",
        content = "[img]./mon_floor_a.jpg[/img] [img]./mon_floor_b.jpg[/img]",
        embeddedComments =
          listOf(
            PostEmbeddedReplyPreview(
              author = "commenter",
              content = "[img]./mon_comment.jpg[/img]",
            ),
          ),
        hotReplies =
          listOf(
            PostEmbeddedReplyPreview(
              author = "hot",
              content = "[img]./mon_hot.jpg[/img]",
            ),
          ),
      )

    assertEquals(
      listOf(
        "https://img.nga.178.com/attachments/mon_floor_a.jpg",
        "https://img.nga.178.com/attachments/mon_floor_b.jpg",
      ),
      postPreviewImageUrls(post),
    )
  }

  @Test
  fun embeddedReplyPreviewImageUrlsOnlyIncludesThatReply() {
    val reply =
      PostEmbeddedReplyPreview(
        author = "commenter",
        content = "[img]./mon_comment_a.jpg[/img] [quote][img]./mon_comment_b.jpg[/img][/quote]",
      )

    assertEquals(
      listOf(
        "https://img.nga.178.com/attachments/mon_comment_a.jpg",
        "https://img.nga.178.com/attachments/mon_comment_b.jpg",
      ),
      embeddedReplyPreviewImageUrls(reply),
    )
  }

  @Test
  fun embeddedReplyOriginalPostUrlIncludesThreadAndPageHints() {
    val reply =
      PostEmbeddedReplyPreview(
        pid = "869524613",
        tid = "25968165",
        floorNumber = 40,
        author = "hot",
        content = "热点回复",
      )

    assertEquals(
      "nga://post/869524613?tid=25968165&page=3",
      embeddedReplyOriginalPostUrl(reply),
    )
  }

  @Test
  fun embeddedReplyOriginalPostUrlReturnsNullWithoutPostId() {
    val reply =
      PostEmbeddedReplyPreview(
        author = "commenter",
        content = "贴条",
      )

    assertEquals(null, embeddedReplyOriginalPostUrl(reply))
  }

  @Test
  fun imagePreviewPageKeysStayUniqueForDuplicateUrls() {
    val repeatedUrl = "https://img.nga.178.com/attachments/mon_202606/01/sample.jpg"

    assertEquals(
      2,
      listOf(
        imagePreviewPageKey(page = 0, url = repeatedUrl),
        imagePreviewPageKey(page = 1, url = repeatedUrl),
      ).toSet().size,
    )
  }

  @Test
  fun coercePreviewPanOffsetLimitsHorizontalPanToScaledViewportEdge() {
    assertEquals(
      Offset(500f, 0f),
      coercePreviewPanOffset(
        offset = Offset(900f, 0f),
        viewportSize = IntSize(width = 500, height = 800),
        scale = 3f,
        imageAspectRatio = null,
      ),
    )
  }

  @Test
  fun coercePreviewPanOffsetLimitsVerticalPanToScaledViewportEdge() {
    assertEquals(
      Offset(0f, -800f),
      coercePreviewPanOffset(
        offset = Offset(0f, -1200f),
        viewportSize = IntSize(width = 500, height = 800),
        scale = 3f,
        imageAspectRatio = null,
      ),
    )
  }

  @Test
  fun coercePreviewPanOffsetRecentersWhenImageIsNotZoomed() {
    assertEquals(
      Offset.Zero,
      coercePreviewPanOffset(
        offset = Offset(100f, -100f),
        viewportSize = IntSize(width = 500, height = 800),
        scale = 1f,
        imageAspectRatio = null,
      ),
    )
  }

  @Test
  fun coercePreviewPanOffsetUsesActualFittedImageBounds() {
    assertEquals(
      Offset(0f, 400f),
      coercePreviewPanOffset(
        offset = Offset(200f, 500f),
        viewportSize = IntSize(width = 500, height = 800),
        scale = 2f,
        imageAspectRatio = 0.25f,
      ),
    )
  }

  @Test
  fun togglePreviewScaleZoomsToFixedScaleWhenImageIsNotZoomed() {
    assertEquals(
      ImagePreviewTransform(scale = 2.5f, offset = Offset.Zero, dragEnabled = true),
      togglePreviewScaleOnDoubleTap(
        scale = 1f,
        offset = Offset(120f, -80f),
        dragEnabled = false,
      ),
    )
  }

  @Test
  fun togglePreviewScaleResetsWhenImageIsZoomed() {
    assertEquals(
      ImagePreviewTransform(scale = 1f, offset = Offset.Zero, dragEnabled = false),
      togglePreviewScaleOnDoubleTap(
        scale = 2.5f,
        offset = Offset(120f, -80f),
        dragEnabled = true,
      ),
    )
  }

  @Test
  fun previewPanConsumesOnlyRealPanChanges() {
    assertEquals(
      false,
      shouldConsumePreviewPanChange(
        scale = 2.5f,
        dragEnabled = true,
        panChange = Offset.Zero,
      ),
    )

    assertEquals(
      true,
      shouldConsumePreviewPanChange(
        scale = 2.5f,
        dragEnabled = true,
        panChange = Offset(1f, 0f),
      ),
    )
  }
}
