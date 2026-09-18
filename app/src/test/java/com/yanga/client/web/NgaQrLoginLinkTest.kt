package com.yanga.client.web

import org.junit.Assert.*
import org.junit.Test

class NgaQrLoginLinkTest {
  private val url = "https://bbs.nga.cn/nuke.php?__lib=login&__act=qrlogin_ui&qrkey=Ab123"

  @Test fun recognizesOfficialQrAndReconstructsUrl() {
    assertEquals(url, NgaQrLoginLink.parse(url)?.url)
    assertEquals("https://ngabbs.com", NgaQrLoginLink.parse(url.replace("bbs.nga.cn", "ngabbs.com"))?.baseUrl)
    assertEquals(url, NgaQrLoginLink.parse("https://bbs.nga.cn/nuke.php?qrkey=Ab123&__act=qrlogin_ui&__lib=login")?.url)
  }

  @Test fun rejectsUntrustedOrAmbiguousAuthorizationTargets() {
    listOf(
      url.replace("https:", "http:"), url.replace("bbs.nga.cn", "bbs.nga.cn.evil.com"),
      url.replace("bbs.nga.cn", "user@bbs.nga.cn"), url.replace("bbs.nga.cn", "bbs.nga.cn:443"),
      "$url#fragment", "$url&qrkey=Other", "$url&%71rkey=Other", "$url&redirect=evil",
      url.replace("qrlogin_ui", "qrlogin_allow"), url.replace("Ab123", ""),
      url.replace("Ab123", "%26evil"), url.replace("/nuke.php", "/other.php"), "not a URL",
    ).forEach { assertNull(it, NgaQrLoginLink.parse(it)) }
  }
}
