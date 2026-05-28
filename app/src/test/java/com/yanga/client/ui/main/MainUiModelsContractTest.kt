package com.yanga.client.ui.main

import com.yanga.client.data.LoginSessionData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MainUiModelsContractTest {
  @Test
  fun loadableUiStateRepresentsLoadingErrorEmptyLoginAndContent() {
    val loading = LoadableUiState.Loading
    val error = LoadableUiState.Error(message = "Network unavailable")
    val empty = LoadableUiState.Empty(message = "No boards yet")
    val loginRequired = LoadableUiState.LoginRequired
    val content = LoadableUiState.Content(value = "ready")

    assertSame(LoadableUiState.Loading, loading)
    assertEquals("Network unavailable", error.message)
    assertEquals("No boards yet", empty.message)
    assertSame(LoadableUiState.LoginRequired, loginRequired)
    assertEquals("ready", content.value)
  }

  @Test
  fun mainContentInitialLoggedOutRequiresLoginForSessionScopedTabs() {
    val state = MainContentUiState.initialLoggedOut()

    assertSame(LoadableUiState.Loading, state.home.activeTopics)
    assertSame(LoadableUiState.Loading, state.home.boards)
    assertSame(LoadableUiState.Loading, state.boards.categories)
    assertSame(LoadableUiState.LoginRequired, state.messages.messages)
    assertSame(LoadableUiState.LoginRequired, state.profile.session)
    assertSame(LoadableUiState.LoginRequired, state.profile.counters)
    assertSame(LoadableUiState.LoginRequired, state.profile.notifications)
    assertEquals(ProfileUiState.defaultSettingsRows, state.profile.settingsRows)
    assertFalse(state.isLoggedIn)
  }

  @Test
  fun mainContentInitialLoggedInStartsReadOnlyTabsInLoadingState() {
    val session = LoginSessionData(username = "reader", uid = "42", cookie = "ngaPassportUid=42")
    val state = MainContentUiState.initialLoggedIn(session)

    assertSame(LoadableUiState.Loading, state.home.activeTopics)
    assertSame(LoadableUiState.Loading, state.home.boards)
    assertSame(LoadableUiState.Loading, state.boards.categories)
    assertSame(LoadableUiState.Loading, state.messages.messages)
    assertEquals(LoadableUiState.Content(session), state.profile.session)
    assertSame(LoadableUiState.Loading, state.profile.counters)
    assertSame(LoadableUiState.Loading, state.profile.notifications)
    assertEquals(ProfileUiState.defaultSettingsRows, state.profile.settingsRows)
    assertTrue(state.isLoggedIn)
  }

  @Test
  fun screenUiStatesCanExposeReadDataContent() {
    val favoriteBoards =
      listOf(BoardPreview(name = "Board", metadata = "metadata", marker = "B", badge = "1"))
    val latestTopics =
      listOf(
        TopicPreview(
          title = "Topic",
          board = "Board",
          replies = "3 replies",
          lastActive = "now",
          authorInitial = "A",
        ),
      )
    val categories = listOf(BoardPreview(name = "Category", metadata = "2 boards", marker = "C"))
    val messages = listOf(MessagePreview(contact = "Mod", preview = "Hello", time = "now"))
    val notifications =
      listOf(
        SettingsPreview(icon = "R", title = "Replies", subtitle = "1 unread", badge = "1"),
      )
    val session = LoginSessionData(username = "reader", uid = "42", cookie = "ngaPassportUid=42")
    val counters = listOf(SettingsPreview("星", "Favorite topics", "3"))

    val state =
      MainContentUiState(
        home =
          HomeUiState(
            boards = LoadableUiState.Content(favoriteBoards),
            activeTopics = LoadableUiState.Content(latestTopics),
          ),
        boards =
          BoardsUiState(
            subscribedBoards = LoadableUiState.Content(emptyList()),
            categories = LoadableUiState.Content(categories),
          ),
        messages =
          MessagesUiState(
            messages = LoadableUiState.Content(messages),
          ),
        profile =
          ProfileUiState(
            session = LoadableUiState.Content(session),
            counters = LoadableUiState.Content(counters),
            notifications = LoadableUiState.Content(notifications),
          ),
      )

    assertEquals(favoriteBoards, (state.home.boards as LoadableUiState.Content).value)
    assertEquals(latestTopics, (state.home.activeTopics as LoadableUiState.Content).value)
    assertEquals(categories, (state.boards.categories as LoadableUiState.Content).value)
    assertEquals(messages, (state.messages.messages as LoadableUiState.Content).value)
    assertEquals(session, (state.profile.session as LoadableUiState.Content).value)
    assertEquals(counters, (state.profile.counters as LoadableUiState.Content).value)
    assertEquals(notifications, (state.profile.notifications as LoadableUiState.Content).value)
  }
}
