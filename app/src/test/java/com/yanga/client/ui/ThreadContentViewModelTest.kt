package com.yanga.client.ui

import com.yanga.client.api.NgaThreadPost
import com.yanga.client.api.NgaThreadRead
import com.yanga.client.api.NgaTopicList
import com.yanga.client.data.BoardsReadData
import com.yanga.client.data.HomeReadData
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.LocalFavoriteBoard
import com.yanga.client.data.MessagesReadData
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.data.ProfileReadData
import com.yanga.client.data.SubBoardVisibilityChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadContentViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun openPageUsesCachedThreadPageWhenAlreadyLoaded() = runTest(dispatcher) {
    val repository = FakeRepository()
    val viewModel = ThreadContentViewModel(repository)

    viewModel.openThread(session = null, destination = ThreadDestination(id = "123", title = "Thread", page = 1))
    advanceUntilIdle()
    viewModel.openPage(session = null, page = 2)
    advanceUntilIdle()
    viewModel.openPage(session = null, page = 1)
    advanceUntilIdle()

    assertEquals(listOf(1, 2), repository.loadedPages)
    assertEquals("1", viewModel.state.value?.page)
    assertEquals("page 1", (viewModel.state.value?.posts as LoadableUiState.Content).value.single().content)
  }

  @Test
  fun openPostOnAlreadyLoadedPageUpdatesTargetWithoutReloadingPage() = runTest(dispatcher) {
    val repository = FakeRepository()
    val viewModel = ThreadContentViewModel(repository)

    viewModel.openThread(session = null, destination = ThreadDestination(id = "123", title = "Thread", page = 1))
    advanceUntilIdle()
    viewModel.openPost(session = null, postId = "p1")
    advanceUntilIdle()

    assertEquals(listOf(1), repository.loadedPages)
    assertEquals("1", viewModel.state.value?.page)
    assertEquals("p1", viewModel.state.value?.targetPostId)
    assertEquals(0, viewModel.state.value?.targetFloorNumber)
  }

  @Test
  fun openSamePostAgainCreatesNewScrollRequestWithoutReloadingPage() = runTest(dispatcher) {
    val repository = FakeRepository()
    val viewModel = ThreadContentViewModel(repository)

    viewModel.openThread(session = null, destination = ThreadDestination(id = "123", title = "Thread", page = 1))
    advanceUntilIdle()
    viewModel.openPost(session = null, postId = "p1")
    advanceUntilIdle()
    val firstScrollRequestId = viewModel.state.value?.targetScrollRequestId ?: 0

    viewModel.openPost(session = null, postId = "p1")
    advanceUntilIdle()

    assertEquals(listOf(1), repository.loadedPages)
    assertEquals("p1", viewModel.state.value?.targetPostId)
    assertTrue((viewModel.state.value?.targetScrollRequestId ?: 0) > firstScrollRequestId)
  }

  @Test
  fun openPostOnKnownPageUsesPageHintWithoutLoadingPostByPid() = runTest(dispatcher) {
    val repository = FakeRepository()
    val viewModel = ThreadContentViewModel(repository)

    viewModel.openThread(session = null, destination = ThreadDestination(id = "123", title = "Thread", page = 1))
    advanceUntilIdle()
    viewModel.openPostOnPage(session = null, postId = "p21", page = 2)
    advanceUntilIdle()

    assertEquals(listOf(1, 2), repository.loadedPages)
    assertEquals(emptyList<String>(), repository.loadedPostIds)
    assertEquals("2", viewModel.state.value?.page)
    assertEquals("p21", viewModel.state.value?.targetPostId)
  }

  @Test
  fun refreshedPollResultsSurviveCachedPageNavigation() = runTest(dispatcher) {
    val poll = com.yanga.client.api.NgaPoll("123", listOf(com.yanga.client.api.NgaPollOption(1, "A", 0)), 1)
    val repository = FakeRepository(poll)
    val viewModel = ThreadContentViewModel(repository)
    viewModel.openThread(null, ThreadDestination("123", "Thread"))
    advanceUntilIdle()
    val updated = poll.copy(options = listOf(poll.options.single().copy(votes = 1)))
    viewModel.updatePollResults(updated)
    viewModel.openPage(null, 2)
    advanceUntilIdle()
    viewModel.openPage(null, 1)
    advanceUntilIdle()
    assertEquals(1L, (viewModel.state.value!!.posts as LoadableUiState.Content).value.single().poll?.totalVotes)
    assertEquals(listOf(1, 2), repository.loadedPages)
  }

  @Test fun authorFilterUsesSeparatePagesAndClearsOnExit() = runTest(dispatcher) {
    val repository = FakeRepository()
    val model = ThreadContentViewModel(repository)
    model.openThread(null, ThreadDestination("123", "Thread"))
    advanceUntilIdle()
    val post = (model.state.value!!.posts as LoadableUiState.Content).value.single()
    model.filterAuthor(null, post)
    advanceUntilIdle()
    model.openPage(null, 2)
    advanceUntilIdle()
    assertEquals(listOf("42" to 1, "42" to 2), repository.filteredPages)
    assertEquals("42", model.state.value!!.filteredAuthorId)
    assertEquals("filtered 42 page 2", (model.state.value!!.posts as LoadableUiState.Content).value.single().content)
    model.openPage(null, 1)
    advanceUntilIdle()
    assertEquals(2, repository.filteredPages.size)
    model.filterAuthor(null, null)
    advanceUntilIdle()
    assertEquals(null, model.state.value!!.filteredAuthorId)
    assertEquals("page 1", (model.state.value!!.posts as LoadableUiState.Content).value.single().content)
  }

  @Test fun updatedScoreSurvivesPageNavigation() = runTest(dispatcher) {
    val model = ThreadContentViewModel(FakeRepository())
    model.openThread(null, ThreadDestination("123", "Thread"))
    advanceUntilIdle()
    model.updatePostScore("p1", -2)
    model.openPage(null, 2)
    advanceUntilIdle()
    model.openPage(null, 1)
    advanceUntilIdle()
    assertEquals(-2, (model.state.value!!.posts as LoadableUiState.Content).value.single().score)
  }

  private class FakeRepository(private val poll: com.yanga.client.api.NgaPoll? = null) : NgaReadOnlyRepository {
    val loadedPages = mutableListOf<Int>()
    val filteredPages = mutableListOf<Pair<String, Int>>()
    override suspend fun loadThreadByAuthor(session: LoginSessionData?, tid: String, page: Int, authorId: String): Result<NgaThreadRead> {
      filteredPages += authorId to page
      return loadThread(session, tid, page).map { it.copy(posts = it.posts.map { post -> post.copy(content = "filtered $authorId page $page") }) }
    }
    val loadedPostIds = mutableListOf<String>()

    override suspend fun loadHome(): Result<HomeReadData> =
      Result.failure(UnsupportedOperationException())

    override suspend fun loadBoards(session: LoginSessionData?): Result<BoardsReadData> =
      Result.failure(UnsupportedOperationException())

    override suspend fun loadMessages(session: LoginSessionData?): Result<MessagesReadData> =
      Result.failure(UnsupportedOperationException())

    override suspend fun loadProfile(session: LoginSessionData?): Result<ProfileReadData> =
      Result.failure(UnsupportedOperationException())

    override suspend fun loadBoardTopics(
      session: LoginSessionData?,
      fid: String,
      page: Int,
      fidGroup: String?,
      recommend: Boolean,
    ): Result<NgaTopicList> =
      Result.failure(UnsupportedOperationException())

    override suspend fun loadThread(session: LoginSessionData?, tid: String, page: Int): Result<NgaThreadRead> {
      loadedPages += page
      return Result.success(
        NgaThreadRead(
          tid = tid,
          subject = "Thread",
          fid = "7",
          page = page,
          replyCount = 39,
          maxPage = 2,
          posts =
            listOf(
              NgaThreadPost(
                pid = "p$page",
                tid = tid,
                fid = "7",
                authorId = "42",
                author = "author",
                subject = "Thread",
                content = "page $page",
                poll = poll.takeIf { page == 1 },
                lou = (page - 1) * 20,
                postDate = 0L,
              ),
            ),
        ),
      )
    }

    override suspend fun loadThreadPost(session: LoginSessionData?, pid: String): Result<NgaThreadPost> {
      loadedPostIds += pid
      return Result.success(
        NgaThreadPost(
          pid = pid,
          tid = "123",
          fid = "7",
          authorId = "42",
          author = "author",
          subject = "Thread",
          content = "page 1",
          lou = 0,
          postDate = 0L,
        ),
      )
    }

    override suspend fun listLocalFavoriteBoards(): Result<List<LocalFavoriteBoard>> =
      Result.success(emptyList())

    override suspend fun addLocalFavoriteBoard(board: LocalFavoriteBoard): Result<Unit> =
      Result.success(Unit)

    override suspend fun removeLocalFavoriteBoard(boardId: String): Result<Unit> =
      Result.success(Unit)

    override suspend fun refreshIncrementalBoardDirectoryIfDue(): Boolean = false

    override suspend fun loadBlockedSubBoards(session: LoginSessionData?, parentFid: String): Result<Set<String>> =
      Result.success(emptySet())

    override suspend fun applySubBoardVisibilityChanges(
      session: LoginSessionData?,
      parentFid: String,
      changes: List<SubBoardVisibilityChange>,
    ): Result<Unit> =
      Result.success(Unit)
  }
}
