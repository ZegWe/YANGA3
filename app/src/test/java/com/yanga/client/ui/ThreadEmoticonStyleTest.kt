package com.yanga.client.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadEmoticonStyleTest {
  @Test
  fun shouldInvertMonochromeEmoticonGroupsForDarkBackgrounds() {
    assertTrue(shouldInvertEmoticonForBackground(Color(0xFF101014), "[s:ac:囧]"))
    assertTrue(shouldInvertEmoticonForBackground(Color(0xFF101014), "[s:a2:goodjob]"))
    assertTrue(shouldInvertEmoticonForBackground(Color(0xFF101014), "[s:dt:ROLL]"))
  }

  @Test
  fun shouldNotInvertColorEmoticonGroupsForDarkBackgrounds() {
    assertFalse(shouldInvertEmoticonForBackground(Color(0xFF101014), "[s:ng:呲牙笑]"))
    assertFalse(shouldInvertEmoticonForBackground(Color(0xFF101014), "[s:pg:哈啤]"))
    assertFalse(shouldInvertEmoticonForBackground(Color(0xFF101014), "[s:pst:举手]"))
  }

  @Test
  fun shouldNotInvertMonochromeEmoticonGroupsForLightBackgrounds() {
    assertFalse(shouldInvertEmoticonForBackground(Color(0xFFFFFBFE), "[s:ac:囧]"))
    assertFalse(shouldInvertEmoticonForBackground(Color(0xFFFFFBFE), "[s:a2:goodjob]"))
    assertFalse(shouldInvertEmoticonForBackground(Color(0xFFFFFBFE), "[s:dt:ROLL]"))
  }
}
