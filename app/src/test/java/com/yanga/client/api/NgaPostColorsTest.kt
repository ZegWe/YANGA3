package com.yanga.client.api
import org.junit.Assert.*
import org.junit.Test
class NgaPostColorsTest {
 @Test fun wardenIsBlueOnlyInModeratedBoard() {
  assertNull(NgaPostColors.color(5, false))
  assertEquals("blue", NgaPostColors.color(5, true))
  assertEquals("purple", NgaPostColors.color(3, false))
  assertEquals("teal", NgaPostColors.color(83, false))
 }
 @Test fun readsOfficialModeratorListAndTemporaryFlags() {
  val html = """commonui.postArg.setDefault(-7,0,44191387,205511,33,"","",",205511,2001t,42,1000t,43,","",null,0)"""
  assertEquals(setOf("205511", "42"), NgaPostColors.moderators(html, 1500))
  assertTrue(NgaPostColors.moderators("<html>unavailable</html>").isEmpty())
 }
}
