package com.yanga.client.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme

@Composable
fun YangaTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  themePreferences: ThemePreferences = ThemePreferences(),
  content: @Composable () -> Unit,
) {
  val resolvedDarkTheme = themePreferences.darkMode.resolveDarkTheme(darkTheme)
  val seedColor = themePreferences.color.seedColor ?: YangaSeedColor
  val useSystemDynamicColor = dynamicColor && themePreferences.color == ThemeColorPreference.System
  val colorScheme =
    when {
      useSystemDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (resolvedDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      else ->
        rememberDynamicColorScheme(
          seedColor = seedColor,
          isDark = resolvedDarkTheme,
        )
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
