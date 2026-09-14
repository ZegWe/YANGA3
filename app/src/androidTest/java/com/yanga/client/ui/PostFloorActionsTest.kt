package com.yanga.client.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PostFloorActionsTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
  private val post = PostPreview(pid = "21", floorNumber = 20, author = "作者", authorId = "42",
    isOriginalPoster = true, floor = "20 楼", time = "今天", content = "楼主的后续回复", avatarInitial = "作", score = 7)

  @Test fun embeddedAndHotReplyAvatarsOpenTheirAuthors() {
    var opened = ""
    val replies = post.copy(
      embeddedComments = listOf(PostEmbeddedReplyPreview(authorId = "77", author = "跟帖作者", content = "评论")),
      hotReplies = listOf(PostEmbeddedReplyPreview(authorId = "88", author = "热评作者", content = "热评")),
    )
    compose.setContent { MaterialTheme {
      ThreadReadingScreen(ThreadUiState(posts = LoadableUiState.Content(listOf(replies))), onBack = {}, onUserClick = { opened = it })
    } }
    compose.onNodeWithContentDescription("查看跟帖作者的资料").performScrollTo().performClick()
    compose.runOnIdle { assertEquals("77", opened) }
    compose.onNodeWithContentDescription("查看热评作者的资料").performScrollTo().performClick()
    compose.runOnIdle { assertEquals("88", opened) }
  }

  @Test fun laterReplyShowsAuthorBadgeAndIndependentFloorNumber() {
    compose.setContent { MaterialTheme {
      ThreadReadingScreen(ThreadUiState(posts = LoadableUiState.Content(listOf(post))), onBack = {})
    } }
    compose.onNodeWithText("楼主").assertExists()
    compose.onNodeWithText("20 楼").assertExists()
    compose.onNodeWithText("7").assertExists()
    val author = compose.onNodeWithText("作者").fetchSemanticsNode().boundsInRoot
    val badge = compose.onNodeWithText("楼主").fetchSemanticsNode().boundsInRoot
    assertTrue(badge.left >= author.right)
    assertTrue(kotlin.math.abs(badge.center.y - author.center.y) < 24f)
    compose.onNodeWithContentDescription("Reply").assertExists()
    compose.onNodeWithText("1/1").assertExists()
    val image = compose.onRoot().captureToImage().asAndroidBitmap()
    File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "post-actions.png").outputStream().use {
      image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
    }
  }

  @Test fun actionsUseExactPostAndServerScoreAndCanChangeDirection() {
    val votes = mutableListOf<Boolean>()
    var replyPid: String? = null
    compose.setContent { MaterialTheme {
      var current by remember { mutableStateOf(post) }
      PostFloorActions(current, { replyPid = it.pid }, { _, support ->
        votes += support
        current = current.copy(score = if (support) 8 else 6)
        Result.success(com.yanga.client.api.NgaReactionResult(score = current.score))
      })
    } }
    compose.onNodeWithText("7").assertExists()
    compose.onNodeWithContentDescription("点赞").assertIsNotSelected()
    compose.onNodeWithContentDescription("点踩").assertIsNotSelected()
    compose.onNodeWithContentDescription("点赞").performClick()
    compose.onNodeWithText("8").assertExists()
    compose.onNodeWithContentDescription("点赞").assertIsSelected().performClick()
    compose.onNodeWithContentDescription("点踩").assertIsNotSelected()
    compose.onNodeWithContentDescription("点踩").performClick()
    compose.onNodeWithText("6").assertExists()
    compose.onNodeWithContentDescription("点赞").assertIsNotSelected()
    compose.onNodeWithContentDescription("点踩").assertIsSelected().performClick()
    compose.onNodeWithContentDescription("点踩").assertIsNotSelected()
    compose.onNodeWithContentDescription("点赞").performClick()
    compose.onNodeWithContentDescription("点踩").performClick()
    compose.onNodeWithContentDescription("点赞").assertIsNotSelected()
    compose.onNodeWithContentDescription("点踩").assertIsSelected()
    compose.onNodeWithContentDescription("回复").performClick()
    compose.runOnIdle {
      assertEquals(listOf(true, true, false, false, true, false), votes)
      assertEquals("21", replyPid)
    }
  }

  @Test fun successfulReactionWithoutFreshScoreUsesOnlyIconState() {
    compose.setContent { MaterialTheme { PostFloorActions(post, {}, { _, _ -> Result.success(com.yanga.client.api.NgaReactionResult()) }) } }
    compose.onNodeWithContentDescription("点赞").performClick()
    compose.onNodeWithContentDescription("点赞").assertIsSelected()
    compose.onNodeWithText("已提交，请刷新查看最新赞数").assertDoesNotExist()
    compose.onNodeWithContentDescription("点赞").performClick()
    compose.onNodeWithContentDescription("点赞").assertIsNotSelected()
  }

  @Test fun serverCancellationOverridesStaleUnselectedIcon() {
    compose.setContent { MaterialTheme { PostFloorActions(post, {}, { _, _ ->
      Result.success(com.yanga.client.api.NgaReactionResult(reaction = 0))
    }) } }
    compose.onNodeWithContentDescription("点赞").performClick()
    compose.onNodeWithContentDescription("点赞").assertIsNotSelected()
    compose.onNodeWithText("赞踩未完成").assertDoesNotExist()
  }

  @Test fun rejectedReactionDoesNotChangeCountAndCanBeRetried() {
    var calls = 0
    compose.setContent { MaterialTheme { PostFloorActions(post, {}, { _, _ ->
      calls++; Result.failure(IllegalStateException("服务端拒绝"))
    }) } }
    compose.onNodeWithContentDescription("点赞").performClick()
    compose.onNodeWithText("服务端拒绝").assertExists()
    compose.onNodeWithText("知道了").performClick()
    compose.onNodeWithContentDescription("点赞").assertIsNotSelected()
    compose.onNodeWithText("7").assertExists()
    compose.onNodeWithContentDescription("点赞").assertIsNotSelected()
    compose.onNodeWithContentDescription("点踩").assertIsNotSelected()
    compose.onNodeWithContentDescription("点赞").performClick()
    compose.runOnIdle { assertEquals(2, calls) }
  }

  @Test fun longPressOpensBottomSheetAndCopiesReadableContent() {
    val body = "[h]活动备注[/h][list][*]参与活动[/list][collapse=说明]隐藏内容[/collapse]"
    compose.setContent { MaterialTheme {
      ThreadReadingScreen(ThreadUiState(posts = LoadableUiState.Content(listOf(post.copy(content = body)))), onBack = {})
    } }
    compose.onNodeWithContentDescription("楼层更多选项").assertDoesNotExist()
    compose.onNodeWithText("活动备注").performTouchInput { longClick() }
    compose.onNodeWithText("楼层操作").assertIsDisplayed()
    compose.onNodeWithText("复制内容").assertIsDisplayed()
    val image = compose.onRoot().captureToImage().asAndroidBitmap()
    File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "post-menu.png").outputStream().use {
      image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
    }
    compose.onNodeWithText("复制内容").performClick()
    compose.onNodeWithText("楼层操作").assertDoesNotExist()
    compose.runOnIdle {
      val context = InstrumentationRegistry.getInstrumentation().targetContext
      val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
      assertEquals("活动备注\n\n• 参与活动\n\n说明\n隐藏内容", clipboard.primaryClip?.getItemAt(0)?.text.toString())
    }
  }

  @Test fun longPressMenuFiltersExactAuthorAndCanBeDismissed() {
    var authorId: String? = null
    compose.setContent { MaterialTheme {
      ThreadReadingScreen(ThreadUiState(posts = LoadableUiState.Content(listOf(post))), onBack = {},
        onFilterAuthor = { authorId = it?.authorId })
    } }
    compose.onNodeWithText(post.content).performTouchInput { longClick() }
    compose.onNodeWithText("只看该作者").performClick()
    compose.onNodeWithText("楼层操作").assertDoesNotExist()
    compose.runOnIdle { assertEquals("42", authorId) }
  }

  @Test fun filterBannerCanReturnToAllAuthors() {
    var cleared = false
    compose.setContent { MaterialTheme {
      ThreadReadingScreen(ThreadUiState(filteredAuthorId = "42", filteredAuthorName = "作者",
        posts = LoadableUiState.Content(listOf(post))), onBack = {}, onFilterAuthor = { cleared = it == null })
    } }
    compose.onNodeWithText("只看：作者").assertExists()
    compose.onNodeWithText("查看全部").performClick()
    compose.runOnIdle { assertTrue(cleared) }
  }
}
