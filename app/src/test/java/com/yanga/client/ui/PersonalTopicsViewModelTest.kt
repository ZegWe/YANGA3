package com.yanga.client.ui

import com.yanga.client.api.NgaPersonalTopic
import com.yanga.client.api.NgaPersonalTopicKind
import com.yanga.client.api.NgaPersonalTopicPage
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import java.lang.reflect.Proxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalTopicsViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val calls = mutableListOf<Pair<String, Int>>()
  private var respond: suspend (String, Int) -> Result<NgaPersonalTopicPage> = { _, page -> success("$page") }
  private val unused = Proxy.newProxyInstance(
    NgaReadOnlyRepository::class.java.classLoader,
    arrayOf(NgaReadOnlyRepository::class.java),
  ) { _, method, _ -> error("Unexpected call: ${method.name}") } as NgaReadOnlyRepository
  private val repository = object : NgaReadOnlyRepository by unused {
    override suspend fun loadUserTopics(session: LoginSessionData?, uid: String, page: Int): Result<NgaPersonalTopicPage> {
      calls += uid to page
      return respond(uid, page)
    }
    override suspend fun loadPersonalTopics(session: LoginSessionData?, kind: NgaPersonalTopicKind, page: Int): Result<NgaPersonalTopicPage> {
      calls += kind.name to page
      return respond(kind.name, page)
    }
  }

  @Before fun setUp() { Dispatchers.setMain(dispatcher) }
  @After fun tearDown() { Dispatchers.resetMain() }

  @Test fun appendsPagesAndRetainsThemOnReturn() = runTest(dispatcher) {
    val model = PersonalTopicsViewModel(repository)
    model.ensureLoaded(NgaPersonalTopicKind.Topics, null, "123")
    advanceUntilIdle()
    model.loadNextPage()
    model.loadNextPage()
    advanceUntilIdle()
    model.ensureLoaded(NgaPersonalTopicKind.Topics, null, "123")
    advanceUntilIdle()
    assertEquals(listOf("1", "2"), model.state.value.items.map { it.tid })
    assertEquals(listOf("123" to 1, "123" to 2), calls)
  }

  @Test fun failedPageRetainsContentAndRetriesSamePage() = runTest(dispatcher) {
    val model = PersonalTopicsViewModel(repository)
    model.ensureLoaded(NgaPersonalTopicKind.Topics, null, "123")
    advanceUntilIdle()
    respond = { _, _ -> Result.failure(IllegalStateException("offline")) }
    model.loadNextPage()
    advanceUntilIdle()
    assertEquals(1, model.state.value.page)
    assertEquals("offline", model.state.value.nextPageError)
    assertEquals(listOf("1"), model.state.value.items.map { it.tid })
    respond = { _, _ -> success("2", hasNext = false) }
    model.loadNextPage()
    advanceUntilIdle()
    model.loadNextPage()
    advanceUntilIdle()
    assertEquals(listOf("123" to 1, "123" to 2, "123" to 2), calls)
    assertNull(model.state.value.nextPageError)
    assertFalse(model.state.value.hasNextPage)
  }

  @Test fun refreshReplacesPagesAndDiscardsLateAppend() = runTest(dispatcher) {
    val model = PersonalTopicsViewModel(repository)
    model.ensureLoaded(NgaPersonalTopicKind.Topics, null, "123")
    advanceUntilIdle()
    val pending = CompletableDeferred<Result<NgaPersonalTopicPage>>()
    respond = { _, page -> if (page == 2) withContext(NonCancellable) { pending.await() } else success("fresh") }
    model.loadNextPage()
    runCurrent()
    model.refresh()
    assertTrue(model.state.value.isRefreshing)
    assertEquals("1", model.state.value.items.single().tid)
    model.loadNextPage()
    runCurrent()
    pending.complete(success("stale"))
    advanceUntilIdle()
    assertEquals(listOf("fresh"), model.state.value.items.map { it.tid })
    assertEquals(1, model.state.value.page)
    assertFalse(model.state.value.isRefreshing)
    assertEquals(listOf("123" to 1, "123" to 2, "123" to 1), calls)
  }

  @Test fun refreshFailureKeepsPreviouslyLoadedPages() = runTest(dispatcher) {
    val model = PersonalTopicsViewModel(repository)
    model.ensureLoaded(NgaPersonalTopicKind.Topics, null, "123")
    advanceUntilIdle()
    model.loadNextPage()
    advanceUntilIdle()
    respond = { _, _ -> Result.failure(IllegalStateException("offline")) }
    model.refresh()
    advanceUntilIdle()
    assertEquals(listOf("1", "2"), model.state.value.items.map { it.tid })
    assertEquals(2, model.state.value.page)
    assertEquals("offline", model.state.value.error)
    assertFalse(model.state.value.isRefreshing)
  }

  @Test fun duplicateOrEmptyPagesStopAutomaticLoading() = runTest(dispatcher) {
    for (empty in listOf(false, true)) {
      respond = { _, page -> if (page == 1 || !empty) success("same") else Result.success(NgaPersonalTopicPage(emptyList(), true)) }
      val model = PersonalTopicsViewModel(repository)
      model.ensureLoaded(NgaPersonalTopicKind.Topics, null, "123")
      advanceUntilIdle()
      model.loadNextPage()
      advanceUntilIdle()
      assertEquals(1, model.state.value.items.size)
      assertFalse(model.state.value.hasNextPage)
    }
  }

  @Test fun changingAuthorClearsPreviousContentAndIgnoresOldRequest() = runTest(dispatcher) {
    val pending = CompletableDeferred<Result<NgaPersonalTopicPage>>()
    respond = { uid, _ -> if (uid == "old") withContext(NonCancellable) { pending.await() } else success("new") }
    val model = PersonalTopicsViewModel(repository)
    model.ensureLoaded(NgaPersonalTopicKind.Topics, null, "old")
    runCurrent()
    model.ensureLoaded(NgaPersonalTopicKind.Topics, null, "new")
    runCurrent()
    pending.complete(success("old"))
    advanceUntilIdle()
    assertEquals("new", model.state.value.items.single().tid)
  }

  @Test fun personalRepliesKeepDifferentPostsFromSameThreadAndResetOnAccountChange() = runTest(dispatcher) {
    respond = { _, page -> Result.success(NgaPersonalTopicPage(
      listOf(NgaPersonalTopic("thread", "post-$page", "Reply", "")), true,
    )) }
    val model = PersonalTopicsViewModel(repository)
    val session = LoginSessionData("user", "1", "cookie")
    model.ensureLoaded(NgaPersonalTopicKind.Replies, session, null)
    advanceUntilIdle()
    model.loadNextPage()
    advanceUntilIdle()
    assertEquals(listOf("post-1", "post-2"), model.state.value.items.map { it.pid })
    assertEquals(listOf("Replies" to 1, "Replies" to 2), calls)
    model.ensureLoaded(NgaPersonalTopicKind.Replies, null, null)
    advanceUntilIdle()
    assertTrue(model.state.value.items.isEmpty())
    assertFalse(model.state.value.hasNextPage)
    assertEquals(2, calls.size)
  }

  private fun success(id: String, hasNext: Boolean = true) = Result.success(
    NgaPersonalTopicPage(listOf(NgaPersonalTopic(id, null, "Topic $id", "")), hasNext),
  )
}
