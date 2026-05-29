package com.yanga.client.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable

@Composable
internal fun YangaBottomNavigation(
  selectedTab: MainTab,
  onTabSelected: (MainTab) -> Unit,
) {
  NavigationBar {
    MainTab.entries.forEach { tab ->
      NavigationBarItem(
        selected = selectedTab == tab,
        onClick = { onTabSelected(tab) },
        icon = { Icon(tab.icon, contentDescription = tab.label) },
      )
    }
  }
}

