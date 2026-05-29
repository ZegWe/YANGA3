package com.yanga.client.ui.main

import com.yanga.client.api.NgaDomains
import com.yanga.client.data.LoginSessionData

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface MainDestinationKey : NavKey {
  @Serializable
  data object Home : MainDestinationKey

  @Serializable
  data object Messages : MainDestinationKey

  @Serializable
  data object Profile : MainDestinationKey

  @Serializable
  data object Login : MainDestinationKey

  @Serializable
  data class Board(
    val fid: String,
  ) : MainDestinationKey

  @Serializable
  data class Thread(
    val tid: String,
  ) : MainDestinationKey
}

enum class MainTab(val label: String, val icon: ImageVector) {
  Home("主页", Icons.Filled.Home),
  Messages("Messages", Icons.Filled.Email),
  Profile("Profile", Icons.Filled.Person),
}

enum class MainTopLevelDestination {
  Home,
  Messages,
  Profile,
}

val MainTab.destination: MainTopLevelDestination
  get() =
    when (this) {
      MainTab.Home -> MainTopLevelDestination.Home
      MainTab.Messages -> MainTopLevelDestination.Messages
      MainTab.Profile -> MainTopLevelDestination.Profile
    }

val MainTopLevelDestination.tab: MainTab
  get() =
    when (this) {
      MainTopLevelDestination.Home -> MainTab.Home
      MainTopLevelDestination.Messages -> MainTab.Messages
      MainTopLevelDestination.Profile -> MainTab.Profile
    }

data class BoardPreview(
  val id: String,
  val name: String,
  val metadata: String,
  val marker: String,
  val badge: String? = null,
  val iconUrl: String? = null,
  val category: String = "",
  val isFavorite: Boolean = false,
)

data class TopicPreview(
  val id: String,
  val title: String,
  val board: String,
  val replies: String,
  val lastActive: String,
  val authorInitial: String,
)

data class MessagePreview(
  val contact: String,
  val preview: String,
  val time: String,
  val badge: String? = null,
)

data class SettingsPreview(
  val icon: String,
  val title: String,
  val subtitle: String,
  val badge: String? = null,
)

sealed interface LoadableUiState<out T> {
  object Loading : LoadableUiState<Nothing>

  data class Error(
    val message: String,
    val cause: Throwable? = null,
  ) : LoadableUiState<Nothing>

  data class Empty(
    val message: String,
  ) : LoadableUiState<Nothing>

  object LoginRequired : LoadableUiState<Nothing>

  data class Content<T>(
    val value: T,
  ) : LoadableUiState<T>
}

data class HomeUiState(
  val boards: LoadableUiState<List<BoardPreview>> = LoadableUiState.Loading,
  val activeTopics: LoadableUiState<List<TopicPreview>> = LoadableUiState.Loading,
)

data class BoardGroupPreview(
  val id: String,
  val name: String,
  val boards: List<BoardPreview>,
)

data class BoardSectionPreview(
  val id: String,
  val name: String,
  val groups: List<BoardGroupPreview>,
)

data class BoardsUiState(
  val subscribedBoards: LoadableUiState<List<BoardPreview>> = LoadableUiState.Loading,
  val sections: LoadableUiState<List<BoardSectionPreview>> = LoadableUiState.Loading,
)

data class MessagesUiState(
  val messages: LoadableUiState<List<MessagePreview>> = LoadableUiState.LoginRequired,
)

data class ProfileUiState(
  val session: LoadableUiState<LoginSessionData> = LoadableUiState.LoginRequired,
  val counters: LoadableUiState<List<SettingsPreview>> = LoadableUiState.LoginRequired,
  val notifications: LoadableUiState<List<SettingsPreview>> = LoadableUiState.LoginRequired,
  val settingsRows: List<SettingsPreview> = defaultSettingsRows,
  val forumEndpoint: String = NgaDomains.BBS_NGA_CN,
) {
  companion object {
    val defaultSettingsRows =
      listOf(
        SettingsPreview("阅", "Reading and appearance", "字体、主题、图片加载"),
        SettingsPreview("缓", "Cache and history", "最近阅读、离线缓存"),
        SettingsPreview("屏", "Block words", "过滤内容和用户"),
      )
  }
}

internal val favoriteBoards: List<BoardPreview> = emptyList()

internal val activeTopics: List<TopicPreview> = emptyList()

internal val subscribedBoards: List<BoardPreview> = emptyList()

internal val forumCategories: List<BoardPreview> = emptyList()

internal val privateMessages: List<MessagePreview> = emptyList()

internal val settingsRows: List<SettingsPreview> = ProfileUiState.defaultSettingsRows

data class PostPreview(
  val author: String,
  val floor: String,
  val time: String,
  val content: String,
  val avatarInitial: String,
)

data class ThreadUiState(
  val title: String = "",
  val page: String = "1",
  val replyCount: String = "0",
  val posts: LoadableUiState<List<PostPreview>> = LoadableUiState.Loading,
)

data class BoardTopicListUiState(
  val boardName: String = "",
  val fid: String = "",
  val iconUrl: String? = null,
  val category: String = "",
  val isFavorite: Boolean = false,
  val topics: LoadableUiState<List<TopicPreview>> = LoadableUiState.Loading,
)

data class MainContentUiState(
  val home: HomeUiState = HomeUiState(),
  val boards: BoardsUiState = BoardsUiState(),
  val messages: MessagesUiState = MessagesUiState(),
  val profile: ProfileUiState = ProfileUiState(),
  val activeBoard: BoardTopicListUiState? = null,
  val activeThread: ThreadUiState? = null,
) {
  val isLoggedIn: Boolean
    get() = profile.session is LoadableUiState.Content

  companion object {
    fun initialLoggedOut(): MainContentUiState =
      MainContentUiState(
        home = HomeUiState(),
        boards = BoardsUiState(),
        messages =
          MessagesUiState(
            messages = LoadableUiState.LoginRequired,
          ),
        profile =
          ProfileUiState(
            session = LoadableUiState.LoginRequired,
            counters = LoadableUiState.LoginRequired,
            notifications = LoadableUiState.LoginRequired,
          ),
      )

    fun initialLoggedIn(session: LoginSessionData): MainContentUiState =
      MainContentUiState(
        home = HomeUiState(),
        boards = BoardsUiState(),
        messages =
          MessagesUiState(
            messages = LoadableUiState.Loading,
          ),
        profile =
          ProfileUiState(
            session = LoadableUiState.Content(session),
            counters = LoadableUiState.Loading,
            notifications = LoadableUiState.Loading,
          ),
      )
  }
}
