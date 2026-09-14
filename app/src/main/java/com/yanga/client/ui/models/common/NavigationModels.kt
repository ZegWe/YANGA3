package com.yanga.client.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Email as OutlinedEmail
import androidx.compose.material.icons.outlined.Home as OutlinedHome
import androidx.compose.material.icons.outlined.Person as OutlinedPerson
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface MainDestinationKey : NavKey {
  @Serializable data object Home : MainDestinationKey
  @Serializable data object Login : MainDestinationKey
  @Serializable data class Board(val destination: BoardDestination, val instanceId: String = java.util.UUID.randomUUID().toString()) : MainDestinationKey
  @Serializable data class Thread(val destination: ThreadDestination, val instanceId: String = java.util.UUID.randomUUID().toString()) : MainDestinationKey
  @Serializable data class Search(val board: BoardDestination? = null, val instanceId: String = java.util.UUID.randomUUID().toString()) : MainDestinationKey
  @Serializable data class Web(val url: String, val title: String, val baseUrl: String, val instanceId: String = java.util.UUID.randomUUID().toString()) : MainDestinationKey
  @Serializable data object AccountSettings : MainDestinationKey
  @Serializable data class User(val uid: String) : MainDestinationKey
  @Serializable data object About : MainDestinationKey
  @Serializable data object ThemeSettings : MainDestinationKey
}

enum class MainTab(
  val label: String,
  val displayLabel: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  Home("Home", "首页", Icons.Filled.Home, Icons.Outlined.OutlinedHome),
  Messages("Messages", "消息", Icons.Filled.Email, Icons.Outlined.OutlinedEmail),
  Profile("Profile", "我的", Icons.Filled.Person, Icons.Outlined.OutlinedPerson),
}

enum class MainTopLevelDestination { Home, Messages, Profile }

val MainTab.destination: MainTopLevelDestination
  get() = when (this) {
    MainTab.Home -> MainTopLevelDestination.Home
    MainTab.Messages -> MainTopLevelDestination.Messages
    MainTab.Profile -> MainTopLevelDestination.Profile
  }

val MainTopLevelDestination.tab: MainTab
  get() = when (this) {
    MainTopLevelDestination.Home -> MainTab.Home
    MainTopLevelDestination.Messages -> MainTab.Messages
    MainTopLevelDestination.Profile -> MainTab.Profile
  }

object MainNavigationSlideSpec {
  const val DurationMillis = 300

  fun forwardEnterOffset(fullWidth: Int): Int = fullWidth

  fun forwardExitOffset(fullWidth: Int): Int = -fullWidth / 3

  fun popEnterOffset(fullWidth: Int): Int = -fullWidth / 3

  fun popExitOffset(fullWidth: Int): Int = fullWidth
}
