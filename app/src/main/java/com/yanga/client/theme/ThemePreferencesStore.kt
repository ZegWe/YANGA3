package com.yanga.client.theme

import android.content.Context

object ThemePreferencesStore {
  private const val PREFS_NAME = "yanga_theme_preferences"
  private const val KEY_DARK_MODE = "dark_mode"
  private const val KEY_COLOR = "color"

  fun load(context: Context): ThemePreferences {
    val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val darkMode =
      preferences.getString(KEY_DARK_MODE, null)?.let { stored ->
        DarkModePreference.entries.firstOrNull { it.name == stored }
      } ?: DarkModePreference.FollowSystem
    val color =
      preferences.getString(KEY_COLOR, null)?.let(::decodeColorPreference)
        ?: ThemeColorPreference.System
    return ThemePreferences(darkMode = darkMode, color = color)
  }

  fun save(context: Context, themePreferences: ThemePreferences) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_DARK_MODE, themePreferences.darkMode.name)
      .putString(KEY_COLOR, encodeColorPreference(themePreferences.color))
      .apply()
  }

  private fun encodeColorPreference(color: ThemeColorPreference): String =
    when (color) {
      ThemeColorPreference.System -> "system"
      is ThemeColorPreference.Fixed -> color.option.name
    }

  private fun decodeColorPreference(value: String): ThemeColorPreference =
    if (value == "system") {
      ThemeColorPreference.System
    } else {
      FixedThemeColor.entries.firstOrNull { it.name == value }?.let(ThemeColorPreference::Fixed)
        ?: ThemeColorPreference.System
    }
}
