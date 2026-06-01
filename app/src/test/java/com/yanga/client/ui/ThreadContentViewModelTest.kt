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

  private class FakeRepository : NgaReadOnlyRepository {
    val loadedPages = mutableListOf<Int>()

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
                lou = (page - 1) * 20,
                postDate = 0L,
              ),
            ),
        ),
      )
    }

    override suspend fun loadThreadPost(session: LoginSessionData?, pid: String): Result<NgaThreadPost> =
      Result.failure(UnsupportedOperationException())

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
