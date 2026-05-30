package com.yanga.client.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NgaAvatarUrlsTest {
  @Test
  fun resolveRelativeAvatarPath() {
    assertEquals(
      "https://img4.nga.178.com/avatars/2002/039/003/000/12345_67890.jpg?1234567890",
      NgaAvatarUrls.resolve(".a/12345_67890.jpg?1234567890", "42"),
    )
  }

  @Test
  fun resolveAbsoluteAvatarUrl() {
    assertEquals(
      "https://img4.nga.178.com/ngabbs/post/smile/ac1.png",
      NgaAvatarUrls.resolve("https://img4.nga.178.com/ngabbs/post/smile/ac1.png"),
    )
  }

  @Test
  fun resolvePipeSeparatedAvatarUsesPrimaryLayer() {
    assertEquals(
      "https://img.nga.178.com/avatars/2002/d7b/b9a/003/62500219_0.jpg?33",
      NgaAvatarUrls.resolve(
        "https://img.nga.178.com/avatars/2002/d7b/b9a/003/62500219_0.jpg?33|.a/62500219_1.jpg?75|.a/62500219_2.jpg?62",
      ),
    )
  }

  @Test
  fun resolveJsonAvatarExtractsEmbeddedHttpUrl() {
    assertEquals(
      "http://pic2.178.com/53/533387/month_1109/93ba4788cc8c7d6c75453fa8a74f3da6.jpg",
      NgaAvatarUrls.resolve(
        """{ "t":1,"l":2,"0":{ "0":"http://pic2.178.com/53/533387/month_1109/93ba4788cc8c7d6c75453fa8a74f3da6.jpg","cX":0.47,"cY":0.78}}""",
      ),
    )
  }

  @Test
  fun resolveBlankAvatarReturnsNull() {
    assertNull(NgaAvatarUrls.resolve(null))
    assertNull(NgaAvatarUrls.resolve(""))
  }
}
