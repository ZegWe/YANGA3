package com.yanga.client.ui

import com.yanga.client.api.NgaThreadRead
import com.yanga.client.api.NgaTopicList
import com.yanga.client.api.NgaTopicSummary
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BoardContentViewModelTest {
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
  fun topicFilterReloadsBoardTopicsWithRecommendFlag() = runTest(dispatcher) {
    val repository = FakeRepository()
    val viewModel = BoardContentViewModel(repository)

    viewModel.openBoard(
      session = null,
      destination = BoardDestination(id = "7", name = "议事厅"),
    )
    advanceUntilIdle()

    viewModel.setTopicFilter(BoardTopicFilter.Recommend)
    advanceTimeBy(300)
    advanceUntilIdle()

    assertEquals(
      listOf(
        BoardTopicRequest(fid = "7", recommend = false),
        BoardTopicRequest(fid = "7", recommend = true),
      ),
      repository.loadBoardTopicRequests,
    )
    assertEquals(BoardTopicFilter.Recommend, viewModel.state.value?.selectedTopicFilter)
  }

  @Test
  fun refreshReloadsCurrentBoardTopics() = runTest(dispatcher) {
    val repository = FakeRepository()
    val viewModel = BoardContentViewModel(repository)

    viewModel.openBoard(
      session = null,
      destination = BoardDestination(id = "7", name = "议事厅"),
    )
    advanceUntilIdle()

    viewModel.refresh()
    advanceUntilIdle()

    assertEquals(
      listOf(
        BoardTopicRequest(fid = "7", recommend = false),
        BoardTopicRequest(fid = "7", recommend = false),
      ),
      repository.loadBoardTopicRequests,
    )
  }

  private class FakeRepository : NgaReadOnlyRepository {
    val loadBoardTopicRequests = mutableListOf<BoardTopicRequest>()

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
    ): Result<NgaTopicList> {
      loadBoardTopicRequests += BoardTopicRequest(fid = fid, recommend = recommend)
      return Result.success(
        NgaTopicList(
          topics =
            listOf(
              NgaTopicSummary(
                topicId = if (recommend) "2002" else "1001",
                boardId = fid,
                boardName = "议事厅",
                title = if (recommend) "精华主题" else "全部主题",
              ),
            ),
          page = page,
          hasNextPage = false,
        ),
      )
    }

    override suspend fun loadThread(session: LoginSessionData?, tid: String, page: Int): Result<NgaThreadRead> =
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
    ): Result<Unit> = Result.success(Unit)
  }

  private data class BoardTopicRequest(
    val fid: String,
    val recommend: Boolean,
  )
}
