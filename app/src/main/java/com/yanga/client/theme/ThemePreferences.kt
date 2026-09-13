package com.yanga.client.theme

import androidx.compose.ui.graphics.Color

data class ThemePreferences(
  val darkMode: DarkModePreference = DarkModePreference.FollowSystem,
  val color: ThemeColorPreference = ThemeColorPreference.System,
)

enum class DarkModePreference(
  val label: String,
) {
  FollowSystem("跟随系统"),
  Light("浅色"),
  Dark("深色");

  fun resolveDarkTheme(systemInDarkTheme: Boolean): Boolean =
    when (this) {
      FollowSystem -> systemInDarkTheme
      Light -> false
      Dark -> true
    }
}

sealed class ThemeColorPreference(
  open val seedColor: Color?,
  open val label: String,
) {
  data object System : ThemeColorPreference(seedColor = null, label = "继承系统")

  data class Fixed(
    val option: FixedThemeColor,
  ) : ThemeColorPreference(seedColor = option.seedColor, label = option.label)
}

enum class FixedThemeColor(
  val label: String,
  val seedColor: Color,
  val previewColors: List<Color>,
  val darkPreviewColors: List<Color>,
) {
  Yanga(
    label = "Yanga",
    seedColor = YangaSeedColor,
    previewColors = listOf(Color(0xFFE2C16D), Color(0xFF7C745F), Color(0xFF9E6B4F)),
    darkPreviewColors = listOf(Color(0xFFD8C58B), Color(0xFFCBC3B1), Color(0xFFE0B59B)),
  ),
  Azure(
    label = "海蓝",
    seedColor = Color(0xFF3D73D9),
    previewColors = listOf(Color(0xFF3D73D9), Color(0xFF6F7790), Color(0xFF7B5EA7)),
    darkPreviewColors = listOf(Color(0xFFB3C7FF), Color(0xFFC7CBE0), Color(0xFFD4BFF6)),
  ),
  Jade(
    label = "青绿",
    seedColor = Color(0xFF2D7D67),
    previewColors = listOf(Color(0xFF2D7D67), Color(0xFF697B72), Color(0xFF7C6B4C)),
    darkPreviewColors = listOf(Color(0xFF8ED8C0), Color(0xFFBBCBC3), Color(0xFFE1C28E)),
  ),
  Rose(
    label = "蔷薇",
    seedColor = Color(0xFFB64D73),
    previewColors = listOf(Color(0xFFB64D73), Color(0xFF8A6D78), Color(0xFF8A6F45)),
    darkPreviewColors = listOf(Color(0xFFFFB0C9), Color(0xFFDABFC9), Color(0xFFE3C48C)),
  ),
  ;

  fun previewColors(isDark: Boolean): List<Color> =
    if (isDark) darkPreviewColors else previewColors
}

enum class ThemeColorSwatchLayout {
  MaterialThreeCircularSplit;

  companion object {
    val default = MaterialThreeCircularSplit
  }
}
