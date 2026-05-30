package com.yanga.client.data.image

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ContentImageRawCacheTest {
  @Test
  fun storeAndGetRoundTrip() {
    val root = Files.createTempDirectory("content-image-cache").toFile()
    root.deleteOnExit()
    val cache =
      ContentImageRawCache(
        cacheDir = root.resolve("raw"),
        indexFile = root.resolve("index.json"),
      )
    val url = "https://img.nga.cn/avatars/test.jpg?123"
    val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

    val stored = cache.store(url, bytes)
    assertNotNull(stored)
    assertTrue(stored!!.exists())

    val loaded = cache.get(url)
    assertNotNull(loaded)
    assertArrayEquals(bytes, loaded!!.readBytes())
    assertTrue(cache.has(url))
  }

  @Test
  fun storeUsesStableFileNameForSameUrl() {
    val root = Files.createTempDirectory("content-image-cache-stable").toFile()
    root.deleteOnExit()
    val cache =
      ContentImageRawCache(
        cacheDir = root.resolve("raw"),
        indexFile = root.resolve("index.json"),
      )
    val url = "https://img.nga.cn/post/pic.png"
    val first = cache.store(url, byteArrayOf(1, 2, 3))
    val second = cache.store(url, byteArrayOf(4, 5, 6))

    assertNotNull(first)
    assertNotNull(second)
    assertEquals(first!!.absolutePath, second!!.absolutePath)
    assertArrayEquals(byteArrayOf(4, 5, 6), second.readBytes())
  }
}
