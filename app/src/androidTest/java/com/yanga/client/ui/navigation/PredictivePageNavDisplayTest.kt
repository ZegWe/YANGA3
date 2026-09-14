package com.yanga.client.ui.navigation

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PredictivePageNavDisplayTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
  private val stack = mutableStateListOf("root", "parent", "child")
  private var childCompositions = 0
  private var childDisposals = 0

  private fun showPages() {
    compose.setContent {
      PredictivePageNavDisplay(
        backStack = stack,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        onBack = { if (stack.size > 1) stack.removeAt(stack.lastIndex) },
        entryProvider = { key ->
          NavEntry(key) {
            var count by rememberSaveable { mutableIntStateOf(0) }
            DisposableEffect(Unit) {
              if (key == "child") childCompositions++
              onDispose { if (key == "child") childDisposals++ }
            }
            Box(Modifier.fillMaxSize().background(if (key == "child") Color.Red else Color.Blue)
              .testTag(key)) {
              Button(onClick = { count++ }) { Text("$key count $count") }
            }
          }
        },
      )
    }
    compose.waitForIdle()
    compose.mainClock.autoAdvance = false
  }

  private fun start(edge: Int = BackEventCompat.EDGE_LEFT) {
    compose.runOnIdle {
      compose.activity.onBackPressedDispatcher.dispatchOnBackStarted(
        BackEventCompat(0f, 300f, 0f, edge),
      )
    }
    compose.mainClock.advanceTimeBy(32)
    // PredictiveBackHandler's flow begins with progress callbacks, not the start callback.
    progress(0f, edge, y = 300f)
  }

  private fun progress(value: Float, edge: Int = BackEventCompat.EDGE_LEFT, y: Float = 400f) {
    compose.runOnIdle {
      compose.activity.onBackPressedDispatcher.dispatchOnBackProgressed(
        BackEventCompat(150f, y, value, edge),
      )
    }
    compose.mainClock.advanceTimeBy(32)
    compose.waitForIdle()
  }

  @Test
  fun dragShrinksAndFollowsWithoutPopping_cancelRestoresSameCompositionAndState() {
    showPages()
    compose.onNodeWithText("child count 0").performClick()
    val initial = compose.onNodeWithTag("child").pageBounds()
    start()
    progress(0.7f)
    val preview = compose.onNodeWithTag("child").pageBounds()
    assertTrue(preview.width < initial.width * 0.95f)
    assertTrue(preview.center.x > initial.center.x)
    assertTrue(preview.center.y > initial.center.y)
    compose.runOnIdle {
      assertEquals(3, stack.size)
      assertEquals(1, childCompositions)
      assertEquals(0, childDisposals)
      compose.activity.onBackPressedDispatcher.dispatchOnBackCancelled()
    }
    compose.mainClock.advanceTimeBy(500)
    compose.waitForIdle()
    assertEquals(initial, compose.onNodeWithTag("child").pageBounds())
    compose.onNodeWithText("child count 1").assertExists()
    compose.runOnIdle {
      assertEquals(3, stack.size)
      assertEquals(1, childCompositions)
      assertEquals(0, childDisposals)
    }
  }

  @Test
  fun fullProgressStaysNearFinger_thenReleaseExitsRightAndPopsExactlyOnce() {
    showPages()
    val initial = compose.onNodeWithTag("child").pageBounds()
    start(BackEventCompat.EDGE_RIGHT)
    progress(1f, BackEventCompat.EDGE_RIGHT)
    val held = compose.onNodeWithTag("child").pageBounds()
    assertTrue(held.center.x < initial.center.x)
    assertTrue(held.center.x > initial.center.x - initial.width * 0.1f)
    compose.mainClock.advanceTimeBy(600)
    compose.runOnIdle {
      assertEquals(3, stack.size)
      compose.activity.onBackPressedDispatcher.onBackPressed()
    }
    compose.mainClock.advanceTimeBy(120)
    val released = compose.onNodeWithTag("child").pageBounds()
    assertTrue(released.center.x > held.center.x)
    compose.runOnIdle { assertEquals(3, stack.size) }
    compose.mainClock.advanceTimeBy(600)
    compose.waitForIdle()
    compose.onNodeWithTag("parent").assertExists()
    compose.runOnIdle {
      assertEquals(listOf("root", "parent"), stack.toList())
      assertEquals(1, childDisposals)
    }
    // The new current page can immediately participate in another gesture.
    start()
    progress(0.4f)
    compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
    compose.mainClock.advanceTimeBy(600)
    compose.waitForIdle()
    compose.runOnIdle { assertEquals(listOf("root"), stack.toList()) }
  }

  @Test
  fun parentBrightensAsGestureAdvancesAndIsFullyBrightAfterCommit() {
    showPages()
    start()
    progress(0.3f, y = 300f)
    fun topBlue(): Float {
      val pixels = compose.onRoot().captureToImage().toPixelMap()
      return pixels[pixels.width / 2, 1].blue
    }
    val early = topBlue()
    progress(0.9f, y = 300f)
    val late = topBlue()
    assertTrue("Parent should brighten ($early -> $late)", late > early + 0.05f)
    assertTrue(late < 0.98f)
    compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
    compose.mainClock.advanceTimeBy(600)
    compose.waitForIdle()
    assertTrue(topBlue() > 0.98f)
  }

  @Test
  fun externalNavigationDuringGestureCannotCommitCapturedParent() {
    showPages()
    start()
    progress(0.5f)
    compose.runOnIdle { stack.add("deep-link") }
    compose.mainClock.advanceTimeBy(600)
    compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
    compose.mainClock.advanceTimeBy(600)
    compose.waitForIdle()
    compose.runOnIdle {
      assertEquals(listOf("root", "parent", "child", "deep-link"), stack.toList())
    }
  }

  @Test
  fun buttonBackWithoutProgressPopsNormally() {
    showPages()
    compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
    compose.mainClock.advanceTimeBy(600)
    compose.waitForIdle()
    compose.onNodeWithTag("parent").assertExists()
    compose.runOnIdle { assertEquals(2, stack.size) }
  }
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.pageBounds(): androidx.compose.ui.geometry.Rect {
  return fetchSemanticsNode().boundsInRoot
}
