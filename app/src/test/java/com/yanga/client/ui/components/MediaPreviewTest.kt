package com.yanga.client.ui.components

import org.junit.Assert.*
import org.junit.Test

class MediaPreviewTest {
  @Test fun parsesOgWithReversedAttributesAndRelativeImage() {
    val result = parseMediaMetadata("https://example.org/posts/1", """<meta content='Music &amp; video' property='og:title'><meta property="og:image" content="/cover.jpg?a=1&amp;b=2">""")
    assertEquals("Music & video", result.first)
    assertEquals("https://example.org/cover.jpg?a=1&b=2", result.second)
  }
  @Test fun twitterFallbackAndUnsafeCover() {
    assertEquals("Song", parseMediaMetadata("https://example.org", "<meta name='twitter:title' content='Song'>").first)
    assertNull(parseMediaMetadata("https://example.org", "<meta property='og:image' content='javascript:alert(1)'>").second)
    assertEquals("Fallback", parseMediaMetadata("https://example.org", "<title>Fallback</title>").first)
  }
  @Test fun durationFormats() {
    assertEquals("00:00", mediaTime(-10))
    assertEquals("01:05", mediaTime(65_000))
    assertEquals("1:01:05", mediaTime(3_665_000))
  }
}
