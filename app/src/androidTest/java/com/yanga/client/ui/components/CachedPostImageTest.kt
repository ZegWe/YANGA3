package com.yanga.client.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CachedPostImageTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun cachedPostImageHasVisibleContainerBeforeImageLoads() {
    composeTestRule.setContent {
      CachedPostImage(url = "https://example.com/post-image.png")
    }

    val imageNode =
      composeTestRule
        .onNodeWithContentDescription("Post image")
        .assertExists()

    val bounds = imageNode.getUnclippedBoundsInRoot()
    assertTrue(bounds.bottom - bounds.top >= 120.dp)
  }
}
