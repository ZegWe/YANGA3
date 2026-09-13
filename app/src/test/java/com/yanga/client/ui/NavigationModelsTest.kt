package com.yanga.client.ui

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationModelsTest {
  @Test
  fun repeatedVisitsHaveIndependentIdentitiesAndSurviveSerialization() {
    val first = MainDestinationKey.Board(BoardDestination("7", "Board"))
    val second = MainDestinationKey.Board(BoardDestination("7", "Board"))
    assertNotEquals(first, second)
    val destinations = listOf<MainDestinationKey>(
      MainDestinationKey.Home, first, second,
      MainDestinationKey.Thread(ThreadDestination("42", "Thread", 3, "99", 12)),
      MainDestinationKey.Search(first.destination),
      MainDestinationKey.Web("https://bbs.nga.cn", "NGA", "https://bbs.nga.cn"),
      MainDestinationKey.Login, MainDestinationKey.ThemeSettings,
    )
    val serializer = kotlinx.serialization.builtins.ListSerializer(MainDestinationKey.serializer())
    val encoded = kotlinx.serialization.json.Json.encodeToString(serializer, destinations)
    assertEquals(destinations, kotlinx.serialization.json.Json.decodeFromString(serializer, encoded))
  }

  @Test
  fun deepLinksRetainPageAndPostTargets() {
    val key = com.yanga.client.ui.navigation.threadDeepLink("https://bbs.nga.cn/read.php?tid=42&page=3&pid=99")
    assertEquals(ThreadDestination("42", "", 3, "99"), key?.destination)
    assertEquals(null, com.yanga.client.ui.navigation.threadDeepLink("https://example.com/"))
  }
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

  @Test
  fun mainDestinationIncludesThemeSettingsDetailPage() {
    val destination: MainDestinationKey = MainDestinationKey.ThemeSettings

    assertEquals(MainDestinationKey.ThemeSettings, destination)
  }

  @Test
  fun mainNavigationSlideOffsetsUseFullWidthDetailEntryAndPartialBackgroundParallax() {
    val width = 900

    assertEquals(300, MainNavigationSlideSpec.DurationMillis)
    assertEquals(width, MainNavigationSlideSpec.forwardEnterOffset(width))
    assertEquals(-width / 3, MainNavigationSlideSpec.forwardExitOffset(width))
    assertEquals(-width / 3, MainNavigationSlideSpec.popEnterOffset(width))
    assertEquals(width, MainNavigationSlideSpec.popExitOffset(width))
  }
}
