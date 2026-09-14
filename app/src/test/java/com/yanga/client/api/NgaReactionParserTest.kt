package com.yanga.client.api
import org.junit.Assert.*
import org.junit.Test
class NgaReactionParserTest {
 @Test fun acceptsServerReactionAndCancellationMessages() {
  for ((message, state) in mapOf("你对这个帖子表示支持" to 1, "你对这个帖子表示反对" to -1,
    "你取消了对这个帖子的支持" to 0, "你取消了对这个帖子的反对" to 0)) {
    assertEquals(state, NgaReactionParser.parse("""{"data":{"0":"$message"}}""").reaction)
  }
 }
 @Test fun serverErrorsStillFail() {
  assertTrue(runCatching { NgaReactionParser.parse("""{"error":{"0":"不能评价自己"}}""") }.isFailure)
  assertTrue(runCatching { NgaReactionParser.parse("""{"data":{"0":"未知结果"}}""") }.isFailure)
 }
}
