package com.yanga.client.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.luminance

private val monochromeEmoticonGroups = setOf("ac", "a2", "dt")
private val emoticonGroupRegex = Regex("""^\[s:([^:\]]+):""", RegexOption.IGNORE_CASE)

internal fun shouldInvertEmoticonForBackground(background: Color, code: String): Boolean =
  background.luminance() < 0.5f && code.emoticonGroup() in monochromeEmoticonGroups

private fun String.emoticonGroup(): String? =
  emoticonGroupRegex.find(trim())?.groupValues?.getOrNull(1)?.lowercase()

internal fun invertedEmoticonColorFilter(): ColorFilter =
  ColorFilter.colorMatrix(
    ColorMatrix(
      floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f,
      ),
    ),
  )
