package com.yanga.client.ui

import com.yanga.client.api.NgaBoardCategory
import com.yanga.client.api.NgaBoardGroup
import com.yanga.client.api.NgaBoardSection
import com.yanga.client.api.NgaBoardSummary
import com.yanga.client.api.NgaMessageSummary
import com.yanga.client.api.NgaNotificationSummary
import com.yanga.client.api.NgaProfileCounters
import com.yanga.client.api.NgaThreadRead
import com.yanga.client.api.NgaTopicEntryTarget
import com.yanga.client.api.NgaTopicEntryType
import com.yanga.client.api.NgaTopicList
import com.yanga.client.api.NgaTopicSummary
import com.yanga.client.data.BoardsReadData
import com.yanga.client.data.HomeReadData
import com.yanga.client.data.LoginRequiredException
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.LocalFavoriteBoard
import com.yanga.client.data.MessagesReadData
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.data.ProfileReadData
import com.yanga.client.data.SubBoardVisibilityChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainContentViewModelTest {
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
  fun loggedOutRefreshLoadsPublicHomeAndBoardsAndKeepsSessionScopedStateLoginRequired() = runTest(dispatcher) {
    val repository = FakeRepository()
    val viewModel = MainContentViewModel(repository)

    viewModel.refresh(loginSession = null)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(listOf(homeBoardPreview()), (state.home.boards as LoadableUiState.Content<*>).value)
    assertEquals(listOf(topicPreview()), (state.home.activeTopics as LoadableUiState.Content<*>).value)
    assertEquals(emptyList<BoardPreview>(), (state.boards.subscribedBoards as LoadableUiState.Content<*>).value)
    assertEquals(listOf(boardSectionPreview()), (state.boards.sections as LoadableUiState.Content<*>).value)
    assertSame(LoadableUiState.LoginRequired, state.messages.messages)
    assertSame(LoadableUiState.LoginRequired, state.profile.session)
    assertSame(LoadableUiState.LoginRequired, state.profile.counters)
    assertSame(LoadableUiState.LoginRequired, state.profile.notifications)
    assertEquals(1, repository.loadHomeCalls)
    assertEquals(listOf(null), repository.loadBoardsSessions)
    assertTrue(repository.loadMessagesSessions.isEmpty())
    assertTrue(repository.loadProfileSessions.isEmpty())
  }

  @Test
  fun loggedInRefreshLoadsMessagesAndProfile() = runTest(dispatcher) {
    val repository = FakeRepository()
    val viewModel = MainContentViewModel(repository)
    val loginSession = LoginSessionUiState(username = "reader", uid = "42", cookie = "ngaPassportUid=42")
    val sessionData = loginSession.toData()

    viewModel.refresh(loginSession)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(listOf(messagePreview()), (state.messages.messages as LoadableUiState.Content<*>).value)
    assertEquals(LoadableUiState.Content(sessionData), state.profile.session)
    assertEquals(counterPreviews(), (state.profile.counters as LoadableUiState.Content<*>).value)
    assertEquals(listOf(notificationPreview()), (state.profile.notifications as LoadableUiState.Content<*>).value)
    assertEquals(listOf(sessionData), repository.loadMessagesSessions)
    assertEquals(listOf(sessionData), repository.loadProfileSessions)
  }

  @Test
  fun loggedInRefreshUsesSubscribedBoardsOnHome() = runTest(dispatcher) {
    val repository = FakeRepository(
      homeResult = Result.success(
        HomeReadData(boards = listOf(publicBoardSummary()), activeTopics = listOf(topicSummary())),
      ),
      boardsResult = Result.success(
        BoardsReadData(subscribedBoards = listOf(boardSummary()), remoteSections = listOf(boardSection())),
      ),
    )
    val viewModel = MainContentViewModel(repository)

    viewModel.refresh(LoginSessionUiState(username = "reader", uid = "42", cookie = "ngaPassportUid=42"))
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(listOf(homeBoardPreview().copy(isFavorite = true)), (state.home.boards as LoadableUiState.Content<*>).value)
    assertEquals(listOf(topicPreview()), (state.home.activeTopics as LoadableUiState.Content<*>).value)
    assertEquals(listOf(homeBoardPreview().copy(isFavorite = true)), (state.boards.subscribedBoards as LoadableUiState.Content<*>).value)
  }

  @Test
  fun repositoryFailureBecomesErrorWithoutFakeFallback() = runTest(dispatcher) {
    val failure = IllegalStateException("network down")
    val repository = FakeRepository(homeResult = Result.failure(failure))
    val viewModel = MainContentViewModel(repository)

    viewModel.refresh(loginSession = null)
    advanceUntilIdle()

    val boards = stateError(viewModel.uiState.value.home.boards)
    val topics = stateError(viewModel.uiState.value.home.activeTopics)
    assertEquals("network down", boards.message)
    assertSame(failure, boards.cause)
    assertEquals("network down", topics.message)
    assertSame(failure, topics.cause)
  }

  @Test
  fun loginRequiredFailureBecomesLoginRequiredState() = runTest(dispatcher) {
    val repository =
      FakeRepository(
        messagesResult = Result.failure(LoginRequiredException()),
        profileResult = Result.failure(LoginRequiredException()),
      )
    val viewModel = MainContentViewModel(repository)

    viewModel.refresh(LoginSessionUiState(username = "reader", uid = "42", cookie = ""))
    advanceUntilIdle()

    assertSame(LoadableUiState.LoginRequired, viewModel.uiState.value.messages.messages)
    assertSame(LoadableUiState.LoginRequired, viewModel.uiState.value.profile.counters)
    assertSame(LoadableUiState.LoginRequired, viewModel.uiState.value.profile.notifications)
  }

  @Test
  fun mapperFormatsTopicBoardMessageNotificationAndCountersIntoPreviews() {
    assertEquals(
      TopicPreview(
        id = "1001",
        title = "Read model wiring",
        board = "开发测试",
        replyCount = 12,
        lastActive = "11-15 06:13",
        authorName = "测试员",
        authorId = "42",
      ),
      topicSummary().toPreview(),
    )
    assertEquals(boardPreview(), boardCategory().toPreview())
    assertEquals(messagePreview(), messageSummary().toPreview())
    assertEquals(notificationPreview(), notificationSummary().toPreview())
    assertEquals(counterPreviews(), profileCounters().toPreviews())
  }

  @Test
  fun mapperRoutesTopicEntryTargetsToBoardDestinations() {
    val boardEntry =
      topicSummary()
        .copy(title = "入口版面", entryTarget = NgaTopicEntryTarget(id = "510407", type = NgaTopicEntryType.Board))
        .toPreview()
    val collectionEntry =
      topicSummary()
        .copy(title = "入口合集", entryTarget = NgaTopicEntryTarget(id = "t39011875", type = NgaTopicEntryType.Collection))
        .toPreview()

    assertEquals(
      TopicNavigationTarget.Board(BoardDestination(id = "510407", name = "入口版面", category = "开发测试")),
      boardEntry.navigationTarget,
    )
    assertEquals(
      TopicNavigationTarget.Board(BoardDestination(id = "t39011875", name = "入口合集", category = "开发测试")),
      collectionEntry.navigationTarget,
    )
  }

  @Test
  fun toggleBoardFavoriteUpdatesFavoriteStateInBoards() = runTest(dispatcher) {
    val repository = FakeRepository()
    val viewModel = MainContentViewModel(repository)
    viewModel.refresh(loginSession = null)
    advanceUntilIdle()

    val board = ((viewModel.uiState.value.boards.sections as LoadableUiState.Content).value.first().groups.first().boards.first())
    viewModel.toggleBoardFavorite(board)
    advanceUntilIdle()

    val updated = ((viewModel.uiState.value.boards.sections as LoadableUiState.Content).value.first().groups.first().boards.first())
    assertTrue(updated.isFavorite)
  }

  private fun stateError(state: LoadableUiState<*>): LoadableUiState.Error {
    assertTrue(state is LoadableUiState.Error)
    return state as LoadableUiState.Error
  }

  @Test
  fun returningToBoardsDoesNotReloadButChangingSessionDoes() = runTest(dispatcher) {
    val repository = FakeRepository()
    val viewModel = BoardsListViewModel(repository)
    viewModel.ensureLoaded(null, "https://bbs.nga.cn")
    advanceUntilIdle()
    val before = viewModel.state.value
    viewModel.ensureLoaded(null, "https://bbs.nga.cn")
    advanceUntilIdle()
    assertSame(before, viewModel.state.value)
    assertEquals(1, repository.loadBoardsSessions.size)
    viewModel.ensureLoaded(LoginSessionData("reader", "42", "cookie"), "https://bbs.nga.cn")
    advanceUntilIdle()
    assertEquals(2, repository.loadBoardsSessions.size)
  }

  @Test
  fun returningToThreadKeepsTheCurrentPageInsteadOfTheEntryPage() = runTest(dispatcher) {
    val viewModel = ThreadContentViewModel(FakeRepository())
    val destination = ThreadDestination("1001", "Thread", page = 1)
    viewModel.ensureThreadOpened(null, destination)
    advanceUntilIdle()
    viewModel.openPage(null, 3)
    advanceUntilIdle()
    val before = viewModel.state.value
    assertEquals("3", before?.page)
    viewModel.ensureThreadOpened(null, destination)
    advanceUntilIdle()
    assertSame(before, viewModel.state.value)
  }

  @Test
  fun returningToBoardKeepsFavoriteAndFilterChanges() = runTest(dispatcher) {
    val viewModel = BoardContentViewModel(FakeRepository())
    val destination = BoardDestination("7", "Board", isFavorite = false)
    viewModel.ensureBoardOpened(null, destination)
    advanceUntilIdle()
    viewModel.setFavorite(true)
    viewModel.setTopicFilter(BoardTopicFilter.Recommend)
    advanceUntilIdle()
    val before = viewModel.state.value
    viewModel.ensureBoardOpened(null, destination)
    advanceUntilIdle()
    assertSame(before, viewModel.state.value)
    assertEquals(true, viewModel.state.value?.isFavorite)
    assertEquals(BoardTopicFilter.Recommend, viewModel.state.value?.selectedTopicFilter)
  }

  private class FakeRepository(
    private val homeResult: Result<HomeReadData> = Result.success(
      HomeReadData(boards = listOf(boardSummary()), activeTopics = listOf(topicSummary())),
    ),
    private val boardsResult: Result<BoardsReadData> = Result.success(
      BoardsReadData(subscribedBoards = emptyList(), remoteSections = listOf(boardSection())),
    ),
    private val messagesResult: Result<MessagesReadData> = Result.success(
      MessagesReadData(messages = listOf(messageSummary())),
    ),
    private val profileResult: Result<ProfileReadData> = Result.success(
      ProfileReadData(counters = profileCounters(), notifications = listOf(notificationSummary())),
    ),
  ) : NgaReadOnlyRepository {
    var loadHomeCalls = 0
    val loadBoardsSessions = mutableListOf<LoginSessionData?>()
    val loadMessagesSessions = mutableListOf<LoginSessionData?>()
    val loadProfileSessions = mutableListOf<LoginSessionData?>()
    private val localFavorites = linkedMapOf<String, LocalFavoriteBoard>()

    override suspend fun loadHome(): Result<HomeReadData> {
      loadHomeCalls += 1
      return homeResult
    }

    override suspend fun loadBoards(session: LoginSessionData?): Result<BoardsReadData> {
      loadBoardsSessions += session
      return boardsResult
    }

    override suspend fun loadMessages(session: LoginSessionData?): Result<MessagesReadData> {
      loadMessagesSessions += session
      return messagesResult
    }

    override suspend fun loadProfile(session: LoginSessionData?): Result<ProfileReadData> {
      loadProfileSessions += session
      return profileResult
    }

    override suspend fun loadBoardTopics(
      session: LoginSessionData?,
      fid: String,
      page: Int,
      fidGroup: String?,
      recommend: Boolean,
    ): Result<NgaTopicList> =
      Result.success(NgaTopicList(topics = emptyList(), page = page, hasNextPage = false))

    override suspend fun loadThread(session: LoginSessionData?, tid: String, page: Int): Result<NgaThreadRead> =
      Result.success(NgaThreadRead(tid = tid, subject = "", fid = "", page = page, posts = emptyList()))

    override suspend fun loadThreadPost(session: LoginSessionData?, pid: String) =
      Result.failure<com.yanga.client.api.NgaThreadPost>(UnsupportedOperationException())

    override suspend fun listLocalFavoriteBoards(): Result<List<LocalFavoriteBoard>> =
      Result.success(localFavorites.values.toList())

    override suspend fun addLocalFavoriteBoard(board: LocalFavoriteBoard): Result<Unit> {
      localFavorites[board.boardId] = board
      return Result.success(Unit)
    }

    override suspend fun removeLocalFavoriteBoard(boardId: String): Result<Unit> {
      localFavorites.remove(boardId)
      return Result.success(Unit)
    }

    override suspend fun refreshIncrementalBoardDirectoryIfDue(): Boolean = false

    override suspend fun loadBlockedSubBoards(session: LoginSessionData?, parentFid: String): Result<Set<String>> =
      Result.success(emptySet())

    override suspend fun applySubBoardVisibilityChanges(
      session: LoginSessionData?,
      parentFid: String,
      changes: List<SubBoardVisibilityChange>,
    ): Result<Unit> = Result.success(Unit)
  }
}

private fun boardSummary(): NgaBoardSummary =
  NgaBoardSummary(
    boardId = "7",
    name = "开发测试",
    description = "客户端开发讨论",
    todayTopicCount = 8,
    unreadCount = 3,
    isSubscribed = true,
  )

private fun publicBoardSummary(): NgaBoardSummary =
  NgaBoardSummary(
    boardId = "100",
    name = "公开推荐板块",
    description = "不应该作为登录首页关注板块",
  )

private fun boardCategory(): NgaBoardCategory =
  NgaBoardCategory(
    id = "dev",
    name = "开发测试",
    boards = listOf(boardSummary()),
  )

private fun boardSection(): NgaBoardSection =
  NgaBoardSection(
    id = "dev",
    name = "开发测试",
    groups =
      listOf(
        NgaBoardGroup(
          id = "dev-group",
          name = "开发讨论组",
          boards = listOf(boardSummary()),
        ),
      ),
  )

private fun topicSummary(): NgaTopicSummary =
  NgaTopicSummary(
    topicId = "1001",
    boardId = "7",
    boardName = "开发测试",
    title = "Read model wiring",
    authorId = "42",
    authorName = "测试员",
    replyCount = 12,
    lastPostAt = 1_700_000_000,
  )

private fun messageSummary(): NgaMessageSummary =
  NgaMessageSummary(
    messageId = "501",
    contactName = "版主",
    subject = "Welcome",
    preview = "Please read the rules",
    lastUpdatedAt = 1_700_000_100,
    unreadCount = 2,
  )

private fun notificationSummary(): NgaNotificationSummary =
  NgaNotificationSummary(
    id = "701",
    title = "Reply",
    preview = "Someone replied to your topic",
    createdAt = 1_700_000_200,
    unreadCount = 4,
  )

private fun profileCounters(): NgaProfileCounters =
  NgaProfileCounters(
    topicCount = 48,
    replyCount = 186,
    favoriteTopics = 5,
    subscribedBoards = 3,
    unreadNotifications = 4,
    unreadMessages = 2,
  )

private fun boardPreview(): BoardPreview =
  BoardPreview(
    id = "dev",
    name = "开发测试",
    metadata = "1 boards",
    marker = "开",
    badge = "3",
  )

private fun homeBoardPreview(): BoardPreview =
  BoardPreview(
    id = "7",
    name = "开发测试",
    metadata = "客户端开发讨论",
    marker = "开",
    badge = "3",
  )

private fun boardSectionPreview(): BoardSectionPreview =
  BoardSectionPreview(
    id = "dev",
    name = "开发测试",
    groups =
      listOf(
        BoardGroupPreview(
          id = "dev-group",
          name = "开发讨论组",
          boards =
            listOf(
              BoardPreview(
                id = "7",
                name = "开发测试",
                metadata = "客户端开发讨论",
                marker = "开",
                badge = "3",
                category = "开发测试 / 开发讨论组",
              ),
            ),
        ),
      ),
  )

private fun topicPreview(): TopicPreview =
  TopicPreview(
    id = "1001",
    title = "Read model wiring",
    board = "开发测试",
    replyCount = 12,
    lastActive = "11-15 06:13",
    authorName = "测试员",
    authorId = "42",
  )

private fun messagePreview(): MessagePreview =
  MessagePreview(
    contact = "版主",
    preview = "Welcome - Please read the rules",
    time = "1700000100",
    badge = "2",
  )

private fun notificationPreview(): SettingsPreview =
  SettingsPreview(
    icon = "R",
    title = "Reply",
    subtitle = "Someone replied to your topic",
    badge = "4",
  )

private fun counterPreviews(): List<SettingsPreview> =
  listOf(
    SettingsPreview("topic", "主题", "48"),
    SettingsPreview("reply", "回复", "186"),
    SettingsPreview("notification", "通知", "4", "4"),
  )


