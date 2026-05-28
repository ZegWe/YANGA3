package com.yanga.client.ui.main

import com.yanga.client.api.NgaBoardCategory
import com.yanga.client.api.NgaBoardSummary
import com.yanga.client.api.NgaMessageSummary
import com.yanga.client.api.NgaNotificationSummary
import com.yanga.client.api.NgaProfileCounters
import com.yanga.client.api.NgaTopicSummary
import com.yanga.client.data.BoardsReadData
import com.yanga.client.data.HomeReadData
import com.yanga.client.data.LoginRequiredException
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.MessagesReadData
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.data.ProfileReadData
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
    assertEquals(listOf(boardPreview()), (state.home.boards as LoadableUiState.Content<*>).value)
    assertEquals(listOf(topicPreview()), (state.home.activeTopics as LoadableUiState.Content<*>).value)
    assertEquals(emptyList<BoardPreview>(), (state.boards.subscribedBoards as LoadableUiState.Content<*>).value)
    assertEquals(listOf(boardPreview()), (state.boards.categories as LoadableUiState.Content<*>).value)
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
        title = "Read model wiring",
        board = "开发测试",
        replies = "12 replies",
        lastActive = "1700000000",
        authorInitial = "测",
      ),
      topicSummary().toPreview(),
    )
    assertEquals(boardPreview(), boardCategory().toPreview())
    assertEquals(messagePreview(), messageSummary().toPreview())
    assertEquals(notificationPreview(), notificationSummary().toPreview())
    assertEquals(counterPreviews(), profileCounters().toPreviews())
  }

  private fun stateError(state: LoadableUiState<*>): LoadableUiState.Error {
    assertTrue(state is LoadableUiState.Error)
    return state as LoadableUiState.Error
  }

  private class FakeRepository(
    private val homeResult: Result<HomeReadData> = Result.success(
      HomeReadData(boards = listOf(boardCategory()), activeTopics = listOf(topicSummary())),
    ),
    private val boardsResult: Result<BoardsReadData> = Result.success(
      BoardsReadData(subscribedBoards = emptyList(), remoteCategories = listOf(boardCategory())),
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
  }
}

private fun boardCategory(): NgaBoardCategory =
  NgaBoardCategory(
    id = "dev",
    name = "开发测试",
    boards =
      listOf(
        NgaBoardSummary(
          boardId = "7",
          name = "开发测试",
          description = "客户端开发讨论",
          todayTopicCount = 8,
          unreadCount = 3,
          isSubscribed = true,
        ),
      ),
  )

private fun topicSummary(): NgaTopicSummary =
  NgaTopicSummary(
    topicId = "1001",
    boardId = "7",
    boardName = "开发测试",
    title = "Read model wiring",
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
    favoriteTopics = 5,
    subscribedBoards = 3,
    unreadNotifications = 4,
    unreadMessages = 2,
  )

private fun boardPreview(): BoardPreview =
  BoardPreview(
    name = "开发测试",
    metadata = "1 boards",
    marker = "开",
    badge = "3",
  )

private fun topicPreview(): TopicPreview =
  TopicPreview(
    title = "Read model wiring",
    board = "开发测试",
    replies = "12 replies",
    lastActive = "1700000000",
    authorInitial = "测",
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
    SettingsPreview("星", "Favorite topics", "5"),
    SettingsPreview("版", "Subscribed boards", "3"),
    SettingsPreview("通", "Unread notifications", "4", "4"),
    SettingsPreview("信", "Unread messages", "2", "2"),
  )
