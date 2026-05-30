package com.yanga.client.data.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class ImageCacheIndexTest {
  @Test
  fun indexPersistsUrlAndFileNameLookups() {
    val indexFile = File.createTempFile("image-cache-index", ".json")
    indexFile.deleteOnExit()

    val index = ImageCacheIndex(indexFile)
    index.put("https://example.com/mon_a.jpg", "/tmp/cache/mon_a.jpg")

    val reloaded = ImageCacheIndex(indexFile)
    assertEquals("/tmp/cache/mon_a.jpg", reloaded.getByUrl("https://example.com/mon_a.jpg"))
    assertEquals("/tmp/cache/mon_a.jpg", reloaded.getByFileName("mon_a.jpg"))
  }

  @Test
  fun getByUnknownUrlReturnsNull() {
    val indexFile = File.createTempFile("image-cache-index-empty", ".json")
    indexFile.deleteOnExit()

    val index = ImageCacheIndex(indexFile)
    assertNull(index.getByUrl("https://example.com/missing.jpg"))
    assertNull(index.getByFileName("missing.jpg"))
  }
}
