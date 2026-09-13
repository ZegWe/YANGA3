package com.yanga.client.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun searchScreenSubmitsQueryAndOpensBoardResult() {
    var submitted = false
    var query = ""
    var openedBoardId: String? = null

    composeTestRule.setContent {
      SearchScreen(
        state =
          SearchUiState(
            query = "",
            results =
              SearchResultsUiState.Boards(
                listOf(BoardPreview(id = "7", name = "议事厅", metadata = "fid: 7", marker = "议")),
              ),
          ),
        onBack = {},
        onQueryChange = { query = it },
        onSubmitSearch = { submitted = true },
        onScopeChange = {},
        onSearchContentChange = {},
        onEssenceOnlyChange = {},
        onBoardClick = { openedBoardId = it.id },
        onTopicClick = {},
      )
    }

    composeTestRule.onNode(hasSetTextAction()).performTextInput("议事厅")
    composeTestRule.onNode(hasSetTextAction()).performImeAction()
    composeTestRule.onNodeWithText("fid: 7", substring = true).performClick()

    assertTrue(submitted)
    assertEquals("议事厅", query)
    assertEquals("7", openedBoardId)
  }

  @Test
  fun searchScreenOpensTopicResult() {
    var openedTopicId: String? = null

    composeTestRule.setContent {
      SearchScreen(
        state =
          SearchUiState(
            scope = SearchScope.Topics,
            results =
              SearchResultsUiState.Topics(
                listOf(
                  TopicPreview(
                    id = "1001",
                    title = "搜索结果",
                    board = "议事厅",
                    replyCount = 5,
                    lastActive = "now",
                  ),
                ),
              ),
          ),
        onBack = {},
        onQueryChange = {},
        onSubmitSearch = {},
        onScopeChange = {},
        onSearchContentChange = {},
        onEssenceOnlyChange = {},
        onBoardClick = {},
        onTopicClick = { openedTopicId = it.id },
      )
    }

    composeTestRule.onNodeWithText("搜索结果").performClick()

    assertEquals("1001", openedTopicId)
  }

  @Test
  fun boardsScreenExposesSearchCallback() {
    var boardsSearchClicked = false

    composeTestRule.setContent {
      BoardsScreen(
        state =
          BoardsUiState(
            subscribedBoards = LoadableUiState.Empty("No favorite boards"),
            sections = LoadableUiState.Empty("No boards"),
          ),
        onSearchClick = { boardsSearchClicked = true },
      )
    }
    composeTestRule.onNodeWithContentDescription("搜索").performClick()
    assertTrue(boardsSearchClicked)
  }

  @Test
  fun boardTopicScreenExposesSearchCallback() {
    var boardTopicSearchClicked = false

    composeTestRule.setContent {
      BoardTopicListScreen(
        state =
          BoardTopicListUiState(
            boardName = "议事厅",
            fid = "7",
            subBoards = LoadableUiState.Empty("No sub-boards"),
            topics = LoadableUiState.Empty("No topics"),
          ),
        onBack = {},
        onTopicClick = {},
        onToggleFavorite = {},
        onSearchClick = { boardTopicSearchClicked = true },
      )
    }
    composeTestRule.onNodeWithContentDescription("搜索").performClick()
    assertTrue(boardTopicSearchClicked)
  }
}
