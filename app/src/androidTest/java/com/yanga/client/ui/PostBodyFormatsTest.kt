package com.yanga.client.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.yanga.client.api.NgaPoll
import com.yanga.client.api.NgaPollOption
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PostBodyFormatsTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test fun publishedPostShowsDiceResultInBody() {
    compose.setContent {
      MaterialTheme {
        ThreadReadingScreen(
          state = ThreadUiState(posts = LoadableUiState.Content(listOf(
            PostPreview(pid = "25", tid = "10", authorId = "1", author = "Test", floor = "2 楼", time = "", avatarInitial = "T",
              content = "投掷结果：[dice]d6[/dice]"),
          ))), onBack = {},
        )
      }
    }
    compose.onNodeWithText("d6(4)", substring = true).assertExists()
    compose.onNodeWithText("[dice]", substring = true).assertDoesNotExist()
  }

  @Test fun signatureUsesFormattedPostRenderer() {
    compose.setContent { MaterialTheme {
      SignatureContent("[b]签名加粗[/b][color=red]红色文字[/color]<br/>[collapse=展开签名]隐藏内容[/collapse]")
    } }
    compose.onNodeWithText("签名加粗", substring = true).assertExists()
    compose.onNodeWithText("[b]", substring = true).assertDoesNotExist()
    val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
    compose.onNodeWithText("签名加粗", substring = true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
    val text = layouts.first().layoutInput.text
    assertTrue(text.spanStyles.any { it.item.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold })
    compose.onNodeWithText("展开签名").performClick()
    compose.onNodeWithText("隐藏内容").assertExists()
  }

  @Test fun mergedRewardTableAndHeadingHaveCompactAlignedLayout() {
    compose.setContent {
      MaterialTheme {
        ThreadReadingScreen(
          state = ThreadUiState(title = "社区活动 · 奖励与参与规则", posts = LoadableUiState.Content(listOf(
            PostPreview(author = "活动管理员", floor = "楼主", time = "今天 12:00", avatarInitial = "活",
              content = "[h]活动备注[/h][list][*]请按照活动要求参与活动[/list]" +
                "[table][tr][td colspan2]社区声望奖励[/td][/tr]\n" +
                "[tr][td rowspan2]符合活动条件的楼层直接奖励[/td][td]75点DOTA2区声望[/td][/tr]\n" +
                "[tr][td]45点DOTA2区声望[/td][/tr][/table]" +
                "[collapse=完整活动说明][list=1][*]按要求报名[*]完成活动领取奖励[/list][/collapse]"),
          ))), onBack = {},
        )
      }
    }
    compose.onNodeWithText("活动备注").assertIsDisplayed()
    compose.onNodeWithText("[h]活动备注[/h]").assertDoesNotExist()
    val first = compose.onNodeWithText("75点DOTA2区声望").fetchSemanticsNode().boundsInRoot
    val second = compose.onNodeWithText("45点DOTA2区声望").fetchSemanticsNode().boundsInRoot
    assertEquals(first.left, second.left, 1f)
    assertTrue(second.top >= first.bottom)
    assertTrue(second.top - first.bottom < 120f)
    compose.onNodeWithText("完整活动说明").performScrollTo().performClick()
    compose.onNodeWithText("完成活动领取奖励").assertExists()
    val screenshot = compose.onRoot().captureToImage().asAndroidBitmap()
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    File(context.cacheDir, "reading-formats.png").outputStream().use {
      screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
    }
  }

  @Test fun realRendererShowsTableCellContentAndStyles() {
    compose.setContent {
      MaterialTheme {
        ThreadReadingScreen(
          state = ThreadUiState(posts = LoadableUiState.Content(listOf(
            PostPreview(author = "Test", floor = "楼主", time = "", avatarInitial = "T",
              content = "[table][tr][td50][b]表头[/b][/td][td]数值[/td][/tr][tr][td][list][*]单元格[/list][/td][td]42[/td][/tr][/table]"),
          ))), onBack = {},
        )
      }
    }
    compose.onNodeWithText("表头").assertExists()
    compose.onNodeWithText("单元格").assertExists()
    compose.onNodeWithText("42").assertExists()
  }

  @Test fun realRendererExpandsNestedListAndCodeWithoutExposingTags() {
    compose.setContent {
      MaterialTheme {
        ThreadReadingScreen(
          state = ThreadUiState(posts = LoadableUiState.Content(listOf(
            PostPreview(author = "Test", floor = "楼主", time = "", avatarInitial = "T",
              content = "[list=1][*]第一项[*]第二项[/list][collapse=详情][code]  x = 1[/code][/collapse]"),
          ))), onBack = {},
        )
      }
    }
    compose.onNodeWithText("1.").assertExists()
    compose.onNodeWithText("第一项").assertExists()
    compose.onNodeWithText("  x = 1").assertDoesNotExist()
    compose.onNodeWithText("详情").performScrollTo().performClick()
    compose.onNodeWithText("  x = 1").assertExists()
    compose.onNodeWithText("详情").performClick()
    compose.onNodeWithText("  x = 1").assertDoesNotExist()
  }

  @Test fun pollEnforcesSelectionLimitAndSubmitsOnlyOnButtonClick() {
    var submittedIds: List<Int>? = null
    var calls = 0
    val poll = NgaPoll("42", listOf(NgaPollOption(11, "选项甲", 3), NgaPollOption(22, "选项乙", 2), NgaPollOption(33, "选项丙", 0)), 2)
    compose.setContent {
      MaterialTheme {
        PostPollCard(poll, onVote = { _, ids ->
          calls++
          submittedIds = ids
          Result.success(poll)
        })
      }
    }
    compose.onNodeWithText("提交投票").assertIsNotEnabled()
    compose.onNodeWithText("选项甲").performClick()
    compose.onNodeWithText("选项乙").performClick()
    compose.onNodeWithText("选项丙").performClick()
    compose.runOnIdle { assertEquals(0, calls) }
    compose.onNodeWithText("提交投票").performClick()
    compose.onNodeWithText("投票成功").assertExists()
    compose.runOnIdle {
      assertEquals(listOf(11, 22), submittedIds)
      assertEquals(1, calls)
    }
    compose.onNodeWithText("提交投票").assertDoesNotExist()
  }

  @Test fun expiredPollShowsResultsWithoutSubmission() {
    compose.setContent {
      MaterialTheme { PostPollCard(NgaPoll("42", listOf(NgaPollOption(11, "是大年", 208), NgaPollOption(22, "不是大年", 31)), 1, endsAt = 1, participants = 239)) }
    }
    compose.onNodeWithText("投票已结束").assertExists()
    compose.onNodeWithText("208 票 · 87.0%").assertExists()
    compose.onNodeWithText("提交投票").assertDoesNotExist()
  }

  @Test fun serverRejectionLeavesSelectionAvailableForCorrection() {
    var calls = 0
    val poll = NgaPoll("42", listOf(NgaPollOption(11, "甲", 0), NgaPollOption(22, "乙", 0)), 1)
    compose.setContent {
      MaterialTheme { PostPollCard(poll, onVote = { _, _ -> calls++; Result.failure(IllegalStateException("服务端拒绝")) }) }
    }
    compose.onNodeWithText("甲").performClick()
    compose.onNodeWithText("乙").performClick()
    compose.onNodeWithText("提交投票").performClick()
    compose.onNodeWithText("服务端拒绝").assertExists()
    compose.onNodeWithText("提交投票").assertIsEnabled()
    compose.runOnIdle { assertEquals(1, calls) }
  }
}
