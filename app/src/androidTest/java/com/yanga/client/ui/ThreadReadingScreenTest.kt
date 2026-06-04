package com.yanga.client.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.click
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ThreadReadingScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun threadReadingScreenDoesNotLeaveLargeGapAfterLastPost() {
    val posts =
      (1..18).map { index ->
        PostPreview(
          author = "reader$index",
          floor = "${index}楼",
          time = "now",
          avatarInitial = "R",
          content = "正文 $index",
        )
      }

    composeTestRule.setContent {
      ThreadReadingScreen(
        state =
          ThreadUiState(
            title = "Long thread",
            page = "1",
            replyCount = posts.size.toString(),
            posts = LoadableUiState.Content(posts),
          ),
        onBack = {},
      )
    }

    composeTestRule
      .onAllNodes(hasScrollAction())[1]
      .performScrollToNode(hasContentDescription("Post card 18楼"))
    repeat(4) {
      composeTestRule.onAllNodes(hasScrollAction())[1].performTouchInput { swipeUp() }
      composeTestRule.waitForIdle()
    }

    val rootBottom = composeTestRule.onRoot().getUnclippedBoundsInRoot().bottom
    val lastPostBottom =
      composeTestRule
        .onNodeWithContentDescription("Post card 18楼")
        .getUnclippedBoundsInRoot()
        .bottom

    val bottomGap = rootBottom - lastPostBottom
    assertTrue(
      "Expected the final post to sit close to the screen bottom, but gap was $bottomGap",
      bottomGap <= 32.dp,
    )
  }

  @Test
  fun threadReadingScreenRendersParsedRichContent() {
    composeTestRule.setContent {
      ThreadReadingScreen(
        state =
          ThreadUiState(
            title = "Rich thread",
            page = "1",
            replyCount = "1",
            posts =
              LoadableUiState.Content(
                listOf(
                  PostPreview(
                    author = "reader",
                    floor = "楼主",
                    time = "now",
                    avatarInitial = "R",
                    content =
                      "[quote][b]quoted[/b][/quote]{align=center]plain [b]bold[/b][/align] [s:ac:囧] [img]./mon_test.jpg[/img] [img]./mon_second.jpg[/img]",
                  ),
                ),
              ),
          ),
        onBack = {},
      )
    }

    composeTestRule.onAllNodesWithText("Rich thread").assertCountEquals(2)
    composeTestRule.onNodeWithText("quoted").assertExists()
    composeTestRule
      .onNodeWithText("plain bold", substring = true, useUnmergedTree = true)
      .assertExists()
    composeTestRule.onAllNodesWithText("align", substring = true).assertCountEquals(0)
    composeTestRule.onAllNodesWithContentDescription("囧").assertCountEquals(1)
    composeTestRule.onAllNodesWithContentDescription("Post image").assertCountEquals(2)
    composeTestRule.onAllNodesWithContentDescription("Post card 楼主").assertCountEquals(1)

    composeTestRule.onAllNodesWithContentDescription("Post image")[0].performClick()
    composeTestRule.onNodeWithContentDescription("Image preview 1 of 2").assertExists()
    composeTestRule.onNodeWithContentDescription("Image preview page 1").assertExists()
    composeTestRule.onNodeWithContentDescription("Image preview page 2").assertExists()

    composeTestRule.onNodeWithContentDescription("Image preview 1 of 2").performTouchInput { swipeLeft() }
    composeTestRule.onNodeWithContentDescription("Image preview 2 of 2").assertExists()

    composeTestRule.onNodeWithContentDescription("Image preview page 2").performTouchInput {
      down(center)
      up()
    }
    composeTestRule.waitUntil(timeoutMillis = 1_000) {
      composeTestRule.onAllNodesWithContentDescription("Close image preview").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithContentDescription("Close image preview").assertExists()
    composeTestRule.onNodeWithContentDescription("Image preview page 2").performTouchInput {
      down(center)
      up()
    }
    composeTestRule.waitUntil(timeoutMillis = 1_000) {
      composeTestRule.onAllNodesWithContentDescription("Close image preview").fetchSemanticsNodes().isEmpty()
    }
    composeTestRule.onAllNodesWithContentDescription("Close image preview").assertCountEquals(0)
    composeTestRule.onNodeWithContentDescription("Image preview page 2").performTouchInput {
      down(center)
      up()
    }
    composeTestRule.waitUntil(timeoutMillis = 1_000) {
      composeTestRule.onAllNodesWithContentDescription("Close image preview").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithContentDescription("Close image preview").performClick()
    composeTestRule.onAllNodesWithContentDescription("Post image")[0].performClick()
    composeTestRule.onNodeWithContentDescription("Image preview 1 of 2").performTouchInput {
      val middle = center
      pinch(
        start0 = middle + Offset(-24f, 0f),
        end0 = middle + Offset(-180f, 0f),
        start1 = middle + Offset(24f, 0f),
        end1 = middle + Offset(180f, 0f),
      )
      swipeLeft()
    }
    composeTestRule.onNodeWithContentDescription("Image preview 1 of 2").assertExists()

    composeTestRule.onNodeWithContentDescription("Image preview page 1").performTouchInput {
      down(center)
      up()
      advanceEventTime(100)
      down(center)
      up()
    }
    composeTestRule.onNodeWithContentDescription("Image preview page 1").performTouchInput {
      down(center)
      up()
    }
    composeTestRule.waitUntil(timeoutMillis = 1_000) {
      composeTestRule.onAllNodesWithContentDescription("Close image preview").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithContentDescription("Close image preview").assertExists()
    composeTestRule.onNodeWithContentDescription("Image preview page 1").performTouchInput {
      down(center)
      up()
    }
    composeTestRule.waitUntil(timeoutMillis = 1_000) {
      composeTestRule.onAllNodesWithContentDescription("Close image preview").fetchSemanticsNodes().isEmpty()
    }
    composeTestRule.onNodeWithContentDescription("Image preview 1 of 2").performTouchInput { swipeLeft() }
    composeTestRule.onNodeWithContentDescription("Image preview 2 of 2").assertExists()
    composeTestRule.onNodeWithContentDescription("Image preview 2 of 2").performTouchInput { swipeRight() }
    composeTestRule.onNodeWithContentDescription("Image preview 1 of 2").assertExists()

    composeTestRule.onNodeWithContentDescription("Image preview page 1").performTouchInput {
      down(center)
      up()
      advanceEventTime(100)
      down(center)
      up()
    }
    composeTestRule.onNodeWithContentDescription("Image preview 1 of 2").performTouchInput { swipeLeft() }
    composeTestRule.onNodeWithContentDescription("Image preview 1 of 2").assertExists()
    composeTestRule.onNodeWithContentDescription("Image preview page 1").performTouchInput {
      down(center)
      up()
      advanceEventTime(100)
      down(center)
      up()
    }
    composeTestRule.onNodeWithContentDescription("Image preview 1 of 2").performTouchInput { swipeLeft() }
    composeTestRule.onNodeWithContentDescription("Image preview 2 of 2").assertExists()
    composeTestRule.onNodeWithContentDescription("Image preview 2 of 2").performTouchInput { swipeRight() }
    composeTestRule.onNodeWithContentDescription("Image preview 1 of 2").assertExists()

    composeTestRule.onNodeWithContentDescription("Image preview page 1").performTouchInput {
      down(center)
      up()
    }
    composeTestRule.waitUntil(timeoutMillis = 1_000) {
      composeTestRule.onAllNodesWithContentDescription("Close image preview").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithContentDescription("Close image preview").performClick()
    composeTestRule.onAllNodesWithContentDescription("Image preview", substring = true).assertCountEquals(0)
  }

  @Test
  fun threadReadingScreenOpensPreviewForNestedQuoteImage() {
    composeTestRule.setContent {
      ThreadReadingScreen(
        state =
          ThreadUiState(
            title = "Nested quote thread",
            page = "1",
            replyCount = "1",
            posts =
              LoadableUiState.Content(
                listOf(
                  PostPreview(
                    author = "reader",
                    floor = "楼主",
                    time = "now",
                    avatarInitial = "R",
                    content =
                      "[quote][quote]intro[/quote][quote]TOP [img]./mon_nested.jpg[/img][/quote][/quote]",
                  ),
                ),
              ),
          ),
        onBack = {},
      )
    }

    composeTestRule.onAllNodesWithContentDescription("Post image").assertCountEquals(1)
    composeTestRule.onAllNodesWithContentDescription("Post image")[0].performClick()
    composeTestRule.onNodeWithContentDescription("Image preview 1 of 1").assertExists()
  }

  @Test
  fun threadReadingScreenRoutesReplyTextToQuotedPostLinkWithoutOriginalPostButton() {
    var clickedUrl = ""
    composeTestRule.setContent {
      ThreadReadingScreen(
        state =
          ThreadUiState(
            title = "Quoted reply thread",
            page = "1",
            replyCount = "1",
            posts =
              LoadableUiState.Content(
                listOf(
                  PostPreview(
                    author = "reader",
                    floor = "1楼",
                    time = "now",
                    avatarInitial = "R",
                    content =
                      "[quote][pid=253176649,12937812,2]Reply[/pid] [b]Post by author:[/b]<br/>quoted text[/quote]body",
                  ),
                ),
              ),
          ),
        onBack = {},
        onLinkClick = { clickedUrl = it },
      )
    }

    composeTestRule.onNodeWithText("[原帖]").assertDoesNotExist()

    composeTestRule.onNodeWithText("Reply Post by author:\nquoted text").performTouchInput {
      click(Offset(96f, 50f))
    }

    assertEquals("nga://post/253176649", clickedUrl)
  }

  @Test
  fun threadReadingScreenCollapsesReplyToPrefixBeforePostLink() {
    var clickedUrl = ""
    composeTestRule.setContent {
      ThreadReadingScreen(
        state =
          ThreadUiState(
            title = "Inline reply thread",
            page = "1",
            replyCount = "1",
            posts =
              LoadableUiState.Content(
                listOf(
                  PostPreview(
                    author = "reader",
                    floor = "1楼",
                    time = "now",
                    avatarInitial = "R",
                    content =
                      "[b]Reply to [pid=253176649,12937812,2]Reply[/pid] Post by [uid=42]author[/uid] (2026-06-01):[/b]<br/>quoted text",
                  ),
                ),
              ),
          ),
        onBack = {},
        onLinkClick = { clickedUrl = it },
      )
    }

    composeTestRule.onNodeWithText("Reply to Reply Post by author (2026-06-01):\nquoted text").assertDoesNotExist()

    composeTestRule.onNodeWithText("Reply Post by author (2026-06-01):\nquoted text").performTouchInput {
      click(Offset(96f, 50f))
    }

    assertEquals("nga://post/253176649", clickedUrl)
  }

  @Test
  fun threadReadingScreenRepeatsScrollWhenTargetRequestChanges() {
    val posts =
      (1..30).map { index ->
        PostPreview(
          pid = "p$index",
          floorNumber = index - 1,
          author = "reader$index",
          floor = "${index}楼",
          time = "now",
          avatarInitial = "R",
          content = "正文 $index",
        )
      }
    val screenState =
      mutableStateOf(
        ThreadUiState(
          title = "Repeated target thread",
          page = "1",
          replyCount = posts.size.toString(),
          targetPostId = "p1",
          targetScrollRequestId = 1,
          posts = LoadableUiState.Content(posts),
        ),
      )

    composeTestRule.setContent {
      ThreadReadingScreen(
        state = screenState.value,
        onBack = {},
      )
    }

    composeTestRule
      .onAllNodes(hasScrollAction())[1]
      .performScrollToNode(hasContentDescription("Post card 25楼"))
    composeTestRule.onNodeWithContentDescription("Post card 25楼").assertExists()

    composeTestRule.runOnUiThread {
      screenState.value = screenState.value.copy(targetScrollRequestId = 2)
    }

    composeTestRule.waitUntil(timeoutMillis = 2_000) {
      composeTestRule.onAllNodesWithContentDescription("Post card 1楼").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithContentDescription("Post card 1楼").assertExists()
  }

  @Test
  fun threadReadingScreenConfirmsAttachmentDownload() {
    var downloadUrl = ""
    composeTestRule.setContent {
      ThreadReadingScreen(
        state =
          ThreadUiState(
            title = "Attachment thread",
            page = "1",
            replyCount = "1",
            posts =
              LoadableUiState.Content(
                listOf(
                  PostPreview(
                    author = "reader",
                    floor = "楼主",
                    time = "now",
                    avatarInitial = "R",
                    content = "正文",
                    attachments =
                      listOf(
                        PostAttachmentPreview(
                          name = "sample image.png",
                          url = "https://img.nga.178.com/attachments/mon_202606/01/sample.png",
                        ),
                      ),
                  ),
                ),
              ),
          ),
        onBack = {},
        onAttachmentDownload = { attachment -> downloadUrl = attachment.url },
      )
    }

    composeTestRule.onNodeWithContentDescription("Attachment sample image.png").performClick()
    composeTestRule.onNodeWithText("下载附件").assertExists()
    composeTestRule.onNodeWithText("保存 sample image.png 到 Downloads？").assertExists()

    composeTestRule.onNodeWithText("下载").performClick()

    assertEquals("https://img.nga.178.com/attachments/mon_202606/01/sample.png", downloadUrl)
  }
}
