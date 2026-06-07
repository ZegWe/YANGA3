package com.yanga.client.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
internal fun YangaBottomNavigation(
  selectedTab: MainTab,
  onTabSelected: (MainTab) -> Unit,
) {
  NavigationBar {
    MainTab.entries.forEach { tab ->
      val selected = selectedTab == tab
      NavigationBarItem(
        selected = selected,
        onClick = { onTabSelected(tab) },
        icon = {
          Icon(
            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = tab.label,
          )
        },
        label = { Text(tab.displayLabel) },
      )
    }
  }
}

