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
  @Serializable data object Messages : MainDestinationKey
  @Serializable data object Profile : MainDestinationKey
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
