package com.yanga.client.ui

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationModelsTest {
  @Test
  fun mainTabsUseDistinctFilledAndOutlinedIcons() {
    MainTab.entries.forEach { tab ->
      assertNotEquals(tab.selectedIcon.name, tab.unselectedIcon.name)
      assertTrue(
        "${tab.name} unselected icon should be outlined",
        tab.unselectedIcon.name.contains("Outlined", ignoreCase = true),
      )
    }
  }
}
