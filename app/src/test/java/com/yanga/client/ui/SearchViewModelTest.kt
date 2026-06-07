package com.yanga.client.ui

import com.yanga.client.api.NgaBoardSummary
import com.yanga.client.api.NgaThreadPost
import com.yanga.client.api.NgaThreadRead
import com.yanga.client.api.NgaTopicList
import com.yanga.client.api.NgaTopicSummary
import com.yanga.client.data.BoardsReadData
import com.yanga.client.data.HomeReadData
import com.yanga.client.data.LocalFavoriteBoard
import com.yanga.client.data.LoginSessionData
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
class SearchViewModelTest {
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
  fun submitGlobalBoardSearchLoadsBoardResults() = runTest {
    val repository =
      FakeSearchRepository(
        boardResults = listOf(NgaBoardSummary(boardId = "7", name = "议事厅", description = "综合讨论")),
      )
    val viewModel = SearchViewModel(repository)
    val session = session()

    viewModel.setSession(session)
    viewModel.updateQuery("议事厅")
    viewModel.submitSearch()
    advanceUntilIdle()

    assertEquals(session, repository.boardSearchSession)
    assertEquals("议事厅", repository.boardSearchQuery)
    val results = viewModel.state.value.results
    assertTrue(results is SearchResultsUiState.Boards)
    assertEquals("7", (results as SearchResultsUiState.Boards).boards.single().id)
  }

  @Test
  fun submitGlobalTopicSearchPassesTopicOptions() = runTest {
    val repository =
      FakeSearchRepository(
        topicResults =
          NgaTopicList(
            topics = listOf(NgaTopicSummary(topicId = "1001", boardId = "7", boardName = "议事厅", title = "搜索结果")),
            page = 1,
            hasNextPage = false,
          ),
      )
    val viewModel = SearchViewModel(repository)

    viewModel.setScope(SearchScope.Topics)
    viewModel.setSearchContent(true)
    viewModel.setEssenceOnly(true)
    viewModel.updateQuery("测试")
    viewModel.submitSearch()
    advanceUntilIdle()

    assertEquals("测试", repository.topicSearchQuery)
    assertEquals(null, repository.topicSearchFid)
    assertEquals(true, repository.topicSearchContent)
    assertEquals(true, repository.topicSearchRecommend)
    val results = viewModel.state.value.results
    assertTrue(results is SearchResultsUiState.Topics)
    assertEquals("1001", (results as SearchResultsUiState.Topics).topics.single().id)
  }

  @Test
  fun submitBoardScopedSearchPassesBoardId() = runTest {
    val repository = FakeSearchRepository(topicResults = NgaTopicList(topics = emptyList(), page = 1, hasNextPage = false))
    val board = BoardDestination(id = "t99", name = "合集")
    val viewModel = SearchViewModel(repository, SearchMode.BoardScoped(board))

    viewModel.updateQuery("心智")
    viewModel.submitSearch()
    advanceUntilIdle()

    assertEquals(SearchScope.Topics, viewModel.state.value.scope)
    assertEquals("t99", repository.topicSearchFid)
    assertTrue(viewModel.state.value.results is SearchResultsUiState.Empty)
  }

  @Test
  fun blankQueryReturnsIdleWithoutCallingRepository() = runTest {
    val repository = FakeSearchRepository()
    val viewModel = SearchViewModel(repository)

    viewModel.updateQuery("   ")
    viewModel.submitSearch()
    advanceUntilIdle()

    assertEquals(null, repository.boardSearchQuery)
    assertEquals(null, repository.topicSearchQuery)
    assertTrue(viewModel.state.value.results is SearchResultsUiState.Idle)
  }

  @Test
  fun longTopicQueryReturnsErrorWithoutCallingRepository() = runTest {
    val repository = FakeSearchRepository()
    val viewModel = SearchViewModel(repository)

    viewModel.setScope(SearchScope.Topics)
    viewModel.updateQuery("长".repeat(41))
    viewModel.submitSearch()
    advanceUntilIdle()

    assertEquals(null, repository.topicSearchQuery)
    val results = viewModel.state.value.results
    assertTrue(results is SearchResultsUiState.Error)
    assertTrue((results as SearchResultsUiState.Error).message.contains("under 40 characters"))
  }

  private fun session(): LoginSessionData =
    LoginSessionData(username = "测试", uid = "42", cookie = "ngaPassportUid=42; ngaPassportCid=abc")

  private class FakeSearchRepository(
    private val boardResults: List<NgaBoardSummary> = emptyList(),
    private val topicResults: NgaTopicList = NgaTopicList(topics = emptyList(), page = 1, hasNextPage = false),
  ) : NgaReadOnlyRepository {
    var boardSearchSession: LoginSessionData? = null
      private set
    var boardSearchQuery: String? = null
      private set
    var topicSearchQuery: String? = null
      private set
    var topicSearchFid: String? = null
      private set
    var topicSearchContent: Boolean? = null
      private set
    var topicSearchRecommend: Boolean? = null
      private set

    override suspend fun searchBoards(session: LoginSessionData?, query: String): Result<List<NgaBoardSummary>> {
      boardSearchSession = session
      boardSearchQuery = query
      return Result.success(boardResults)
    }

    override suspend fun searchTopics(
      session: LoginSessionData?,
      query: String,
      fid: String?,
      page: Int,
      searchContent: Boolean,
      recommend: Boolean,
    ): Result<NgaTopicList> {
      topicSearchQuery = query
      topicSearchFid = fid
      topicSearchContent = searchContent
      topicSearchRecommend = recommend
      return Result.success(topicResults)
    }

    override suspend fun loadHome(): Result<HomeReadData> = unsupported()

    override suspend fun loadBoards(session: LoginSessionData?): Result<BoardsReadData> = unsupported()

    override suspend fun loadMessages(session: LoginSessionData?): Result<MessagesReadData> = unsupported()

    override suspend fun loadProfile(session: LoginSessionData?): Result<ProfileReadData> = unsupported()

    override suspend fun loadBoardTopics(
      session: LoginSessionData?,
      fid: String,
      page: Int,
      fidGroup: String?,
      recommend: Boolean,
    ): Result<NgaTopicList> = unsupported()

    override suspend fun loadThread(session: LoginSessionData?, tid: String, page: Int): Result<NgaThreadRead> = unsupported()

    override suspend fun loadThreadPost(session: LoginSessionData?, pid: String): Result<NgaThreadPost> = unsupported()

    override suspend fun listLocalFavoriteBoards(): Result<List<LocalFavoriteBoard>> = unsupported()

    override suspend fun addLocalFavoriteBoard(board: LocalFavoriteBoard): Result<Unit> = unsupported()

    override suspend fun removeLocalFavoriteBoard(boardId: String): Result<Unit> = unsupported()

    override suspend fun refreshIncrementalBoardDirectoryIfDue(): Boolean = false

    override suspend fun loadBlockedSubBoards(session: LoginSessionData?, parentFid: String): Result<Set<String>> = unsupported()

    override suspend fun applySubBoardVisibilityChanges(
      session: LoginSessionData?,
      parentFid: String,
      changes: List<SubBoardVisibilityChange>,
    ): Result<Unit> = unsupported()

    private fun <T> unsupported(): Result<T> =
      Result.failure(UnsupportedOperationException("Not used by SearchViewModelTest"))
  }
}
