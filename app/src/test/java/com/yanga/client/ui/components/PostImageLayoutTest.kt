package com.yanga.client.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class PostImageLayoutTest {
  @Test
  fun postImageHeightFollowsImageAspectRatio() {
    assertEquals(300.dp, postImageHeight(containerWidth = 600.dp, aspectRatio = 2f))
  }

  @Test
  fun postImageHeightCapsVeryTallImages() {
    assertEquals(520.dp, postImageHeight(containerWidth = 600.dp, aspectRatio = 0.5f))
  }

  @Test
  fun postImageHeightKeepsLoadingPlaceholderCompact() {
    assertEquals(120.dp, postImageHeight(containerWidth = 600.dp, aspectRatio = null))
  }
}
