package com.yanga.client.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface MainDestinationKey : NavKey {
  @Serializable data object Home : MainDestinationKey
  @Serializable data object Messages : MainDestinationKey
  @Serializable data object Profile : MainDestinationKey
  @Serializable data object Login : MainDestinationKey
  @Serializable data class Board(val fid: String) : MainDestinationKey
  @Serializable data class Thread(val tid: String) : MainDestinationKey
}

enum class MainTab(val label: String, val icon: ImageVector) {
  Home("主页", Icons.Filled.Home),
  Messages("Messages", Icons.Filled.Email),
  Profile("Profile", Icons.Filled.Person),
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
