package com.yanga.client.ui

import androidx.activity.ComponentActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.yanga.client.api.NgaPersonalTopic
import com.yanga.client.api.NgaPersonalTopicPage
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.ui.navigation.PredictivePageNavDisplay
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PersonalTopicsScreenTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun scrollingAppendsTopicsAndPullDownRefreshesFirstPage() {
    val requests = mutableListOf<Int>()
    val unused = Proxy.newProxyInstance(
      NgaReadOnlyRepository::class.java.classLoader,
      arrayOf(NgaReadOnlyRepository::class.java),
    ) { _, method, _ -> error("Unexpected call: ${method.name}") } as NgaReadOnlyRepository
    val repository = object : NgaReadOnlyRepository by unused {
      override suspend fun loadUserTopics(session: LoginSessionData?, uid: String, page: Int): Result<NgaPersonalTopicPage> {
        requests += page
        return Result.success(NgaPersonalTopicPage(
          List(30) { NgaPersonalTopic("$page-$it", null, "Page $page topic $it", "Excerpt") },
          hasNextPage = page == 1,
        ))
      }
    }
    compose.setContent {
      PersonalTopicsScreen("Topics", repository, null, {}, {}, {}, authorUid = "123")
    }
    compose.onNodeWithText("上一页").assertDoesNotExist()
    compose.onNodeWithText("下一页").assertDoesNotExist()
    compose.onNodeWithContentDescription("刷新本页").assertDoesNotExist()
    compose.onNode(hasScrollToIndexAction()).performScrollToIndex(29)
    compose.runOnIdle { assertEquals(listOf(1, 2), requests) }
    compose.onNode(hasScrollToIndexAction()).performScrollToIndex(30)
    compose.onNodeWithText("Page 2 topic 0").assertExists()
    compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
    compose.onNode(hasScrollToIndexAction()).performTouchInput { swipeDown() }
    compose.runOnIdle { assertEquals(listOf(1, 2, 1), requests) }
    compose.onNodeWithText("Page 1 topic 0").assertExists()
  }

  @Test
  fun returningFromThreadRetainsScrollAndDoesNotReloadTopics() {
    var requests = 0
    val unusedRepository = Proxy.newProxyInstance(
      NgaReadOnlyRepository::class.java.classLoader,
      arrayOf(NgaReadOnlyRepository::class.java),
    ) { _, method, _ -> error("Unexpected repository call: ${method.name}") } as NgaReadOnlyRepository
    val repository = object : NgaReadOnlyRepository by unusedRepository {
      override suspend fun loadUserTopics(session: LoginSessionData?, uid: String, page: Int): Result<NgaPersonalTopicPage> {
        requests++
        return Result.success(NgaPersonalTopicPage(
          items = List(40) { NgaPersonalTopic("$it", null, "Topic $it", "Excerpt $it") },
          hasNextPage = false,
        ))
      }
    }
    val stack = mutableStateListOf("topics")
    compose.setContent {
      PredictivePageNavDisplay(
        backStack = stack,
        entryDecorators = listOf(
          rememberSaveableStateHolderNavEntryDecorator(),
          rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        onBack = { stack.removeAt(stack.lastIndex) },
        entryProvider = { key ->
          NavEntry(key) {
            if (key == "topics") {
              PersonalTopicsScreen("Topics", repository, null, {}, {}, { stack.add("thread") }, authorUid = "123")
            } else {
              Text("Thread details")
            }
          }
        },
      )
    }
    compose.onNode(hasScrollToIndexAction()).performScrollToIndex(20)
    val before = compose.onNodeWithText("Topic 20").fetchSemanticsNode().boundsInRoot
    compose.onNodeWithText("Topic 20").performClick()
    compose.onNodeWithText("Thread details").assertExists()
    compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
    val after = compose.onNodeWithText("Topic 20").fetchSemanticsNode().boundsInRoot
    assertEquals(before, after)
    compose.runOnIdle { assertEquals(1, requests) }
  }
}
