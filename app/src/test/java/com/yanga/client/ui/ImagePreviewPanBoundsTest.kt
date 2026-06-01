package com.yanga.client.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class ImagePreviewPanBoundsTest {
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
}
