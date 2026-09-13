package com.yanga.client.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePreferencesTest {
  @Test
  fun defaultThemePreferencesFollowSystemAndUseSystemColor() {
    val preferences = ThemePreferences()

    assertEquals(DarkModePreference.FollowSystem, preferences.darkMode)
    assertEquals(ThemeColorPreference.System, preferences.color)
  }

  @Test
  fun darkModePreferenceResolvesAgainstSystemDarkState() {
    assertTrue(DarkModePreference.FollowSystem.resolveDarkTheme(systemInDarkTheme = true))
    assertFalse(DarkModePreference.FollowSystem.resolveDarkTheme(systemInDarkTheme = false))
    assertFalse(DarkModePreference.Light.resolveDarkTheme(systemInDarkTheme = true))
    assertTrue(DarkModePreference.Dark.resolveDarkTheme(systemInDarkTheme = false))
  }

  @Test
  fun themeColorOptionsExposeSystemAndFixedMaterialTriplets() {
    assertNull(ThemeColorPreference.System.seedColor)
    assertTrue(FixedThemeColor.entries.size >= 4)
    FixedThemeColor.entries.forEach { option ->
      assertEquals(3, option.previewColors.size)
    }
  }

  @Test
  fun themeColorSwatchUsesMaterialThreeCircularSplitLayout() {
    assertEquals(ThemeColorSwatchLayout.MaterialThreeCircularSplit, ThemeColorSwatchLayout.default)
  }

  @Test
  fun fixedThemeColorPreviewChangesForDarkAndLightMode() {
    FixedThemeColor.entries.forEach { option ->
      assertEquals(3, option.previewColors(isDark = false).size)
      assertEquals(3, option.previewColors(isDark = true).size)
      assertTrue(option.previewColors(isDark = false) != option.previewColors(isDark = true))
    }
  }
}
