package com.yanga.client

import androidx.test.platform.app.InstrumentationRegistry
import coil.request.SuccessResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in real-network regression: -e liveNgaImages true. No account credentials required. */
class SignatureImageLoadingTest {
  @Test fun imageRefererFollowsSelectedForum() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val app = context.applicationContext as YangaApplication
    val original = app.repository.currentBaseUrl()
    try {
      for (endpoint in listOf("https://bbs.nga.cn", "https://ngabbs.com", "https://nga.178.com")) {
        app.repository.setBaseUrl(endpoint)
        val request = app.imageCacheManager.buildContentImageRequest(context, "https://img.nga.cn/attachments/example.png")
        assertEquals("$endpoint/", request.headers["Referer"])
      }
    } finally {
      app.repository.setBaseUrl(original)
    }
  }

  @Test fun loadsLegacySignaturePngsAndGifs() = runBlocking {
    assumeTrue(InstrumentationRegistry.getArguments().getString("liveNgaImages") == "true")
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val manager = (context.applicationContext as YangaApplication).imageCacheManager
    val urls = listOf(
      ".u/9249955Q2bpuhxip1uK1xSf8-2j.png",
      "https://img.nga.178.com/attachments/mon_202409/14/-5i9b7Q0-fznnT1kSf5-2s.png",
      "./mon_202307/10/-5i9b7Q2s-7ptqXpZ6zT3cSb2-7u.gif",
      "./mon_202307/10/-5i9b7Q2s-i7kpXqZ79T3cSgb-bx.gif",
    )
    for (url in urls) {
      val file = manager.ensureRawContentCached(url)
      assertNotNull("Download failed: $url", file)
      val result = manager.imageLoader.execute(manager.buildContentImageRequest(context, url, data = file))
      assertTrue("Decode failed: $url ($result)", result is SuccessResult)
      val drawable = (result as SuccessResult).drawable
      assertTrue(drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0)
      if (url.endsWith(".gif")) assertTrue("GIF must animate", drawable is android.graphics.drawable.Animatable)
    }
  }
}
