package com.yanga.client.ui

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import com.yanga.client.ui.components.PostAudioPlayer
import com.yanga.client.ui.components.PostVideoPlayer
import com.yanga.client.ui.content.PostContentPart
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class PostMediaTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private fun media(name: String): String {
    val context = compose.activity
    val target = File(context.cacheDir, "test-$name")
    InstrumentationRegistry.getInstrumentation().context.assets.open("media/$name").use { input ->
      target.outputStream().use { input.copyTo(it) }
    }
    return Uri.fromFile(target).toString()
  }

  @Test fun audioStartsOnlyOnTapAndCanPauseAndResume() {
    val url = media("sample.wav")
    var opened: String? = null
    compose.setContent { MaterialTheme { PostAudioPlayer(url, "测试声音", onOpenLink = { opened = it }) } }
    compose.onNodeWithContentDescription("Play audio").assertExists().performClick()
    compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Pause audio").fetchSemanticsNodes().isNotEmpty() }
    compose.onNodeWithContentDescription("Pause audio").performClick()
    compose.onNodeWithContentDescription("音频进度").performSemanticsAction(SemanticsActions.SetProgress) { it(10_000f) }
    compose.waitUntil(5_000) {
      compose.onNodeWithContentDescription("音频进度").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current >= 9_000f
    }
    compose.onNodeWithContentDescription("Play audio").performClick()
    compose.onNodeWithContentDescription("Pause audio").assertExists()
    compose.onNodeWithText("打开原链接").performClick()
    compose.runOnIdle { assertEquals(url, opened) }
  }

  @Test fun videoStartsOnTapAndStopsWhenContentLeavesComposition() {
    val url = media("sample.mp4")
    val visible = mutableStateOf(true)
    compose.setContent { MaterialTheme {
      if (visible.value) PostVideoPlayer(PostContentPart.Video(url, "测试视频", true), {}) else Text("已离开")
    } }
    assertNull(findVideo(compose.activity.window.decorView))
    compose.onNodeWithText("播放视频").performClick()
    var video: VideoView? = null
    compose.waitUntil(10_000) {
      onView(isAssignableFrom(VideoView::class.java)).check { view, _ -> video = view as VideoView }
      video?.isPlaying == true
    }
    compose.onNodeWithContentDescription("全屏视频").assertExists()
    compose.onNodeWithContentDescription("全屏视频").performTouchInput { click(androidx.compose.ui.geometry.Offset(center.x, height * .25f)) }
    compose.onNodeWithContentDescription("关闭视频").assertDoesNotExist()
    compose.onNodeWithContentDescription("视频进度").assertDoesNotExist()
    compose.runOnIdle { assertTrue(video!!.isPlaying) }
    compose.onNodeWithContentDescription("全屏视频").performTouchInput { click(center) }
    compose.onNodeWithContentDescription("关闭视频").assertExists()
    compose.onNodeWithContentDescription("暂停视频").performClick()
    compose.onNodeWithContentDescription("视频进度").assertExists()
    // Deliberately seek between frames, rather than to an existing keyframe.
    compose.onNodeWithContentDescription("视频进度").performSemanticsAction(SemanticsActions.SetProgress) { it(4_350f) }
    compose.waitUntil(5_000) { kotlin.math.abs(video!!.currentPosition - 4_350) <= 150 }
    compose.runOnIdle { assertFalse(video!!.isPlaying) }
    compose.onNodeWithContentDescription("继续播放视频").performClick()
    compose.waitUntil(5_000) { video!!.currentPosition >= 4_600 }
    compose.onNodeWithContentDescription("暂停视频").performClick()
    compose.runOnIdle { assertTrue(video!!.currentPosition >= 4_350) }
    compose.onNodeWithContentDescription("关闭视频").performClick()
    compose.onNodeWithContentDescription("全屏视频").assertDoesNotExist()
    compose.runOnIdle { assertFalse(video!!.isPlaying) }
    compose.onNodeWithText("播放视频").performClick()
    compose.runOnIdle { visible.value = false }
    compose.onNodeWithText("已离开").assertExists()
    compose.runOnIdle { assertFalse(video!!.isPlaying) }
  }

  @Test fun localVideoHasCoverBeforePlaying() {
    val url = media("sample.mp4")
    compose.setContent { MaterialTheme { PostVideoPlayer(PostContentPart.Video(url, "封面测试", true), {}) } }
    compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("视频封面").fetchSemanticsNodes().isNotEmpty() }
    assertNull(findVideo(compose.activity.window.decorView))
  }

  @Test fun embeddedProviderHasVisibleLinkAndDoesNotCreateNativePlayer() {
    val url = "https://www.bilibili.com/video/BV1example"
    var opened: String? = null
    compose.setContent { MaterialTheme { PostVideoPlayer(PostContentPart.Video(url, "BV1example", false), { opened = it }) } }
    compose.onNodeWithText("播放视频").assertDoesNotExist()
    compose.onNodeWithText("打开媒体链接").performClick()
    compose.runOnIdle { assertEquals(url, opened); assertNull(findVideo(compose.activity.window.decorView)) }
  }

  @Test fun videosRenderInsideCollapsedPostBody() {
    compose.setContent { MaterialTheme {
      SignatureContent("[collapse=展开视频][flash=video]https://example.com/clip.mp4[/flash][/collapse]")
    } }
    compose.onNodeWithText("播放视频").assertDoesNotExist()
    compose.onNodeWithText("展开视频").performClick()
    compose.onNodeWithText("播放视频").assertExists()
  }

  private fun findVideo(view: View): VideoView? {
    if (view is VideoView) return view
    if (view is ViewGroup) for (i in 0 until view.childCount) findVideo(view.getChildAt(i))?.let { return it }
    return null
  }
}
