package com.yanga.client.api

import org.junit.Assert.*
import org.junit.Test

class NgaPollTest {
  private val sample = "209185~是大年~209186~不是大年~max_select~1~end~1788704956~_209185~208,0,239~_209186~31,0,0"

  @Test fun parsesCapturedPollOptionsTotalsAndDeadline() {
    val poll = NgaPollParser.parse("47470820", sample)!!
    assertEquals(listOf(209185, 209186), poll.options.map { it.id })
    assertEquals(listOf(208L, 31L), poll.options.map { it.votes })
    assertEquals(239L, poll.totalVotes)
    assertEquals(239L, poll.participants)
    assertEquals(1, poll.maxSelections)
    assertTrue(poll.isClosed(1788704956))
    assertFalse(poll.isClosed(1788704955))
  }

  @Test fun readsVoteFieldThroughThreadParser() {
    val json = """{"data":{"__T":{"tid":47470820},"__R":{"0":{"pid":0,"content":"正文","vote":"$sample"}}}}"""
    val post = NgaThreadParser.parseRead(json).posts.single()
    assertEquals(239L, post.poll?.totalVotes)
    assertEquals("正文", post.content)
  }

  @Test fun validatesSelectionWithoutTreatingOptionIdsAsArrayIndexes() {
    val poll = NgaPollParser.parse("1", sample)!!
    assertNull(poll.validationError(listOf(209185), 1))
    assertNotNull(poll.validationError(emptyList(), 1))
    assertNotNull(poll.validationError(listOf(0), 1))
    assertNotNull(poll.validationError(listOf(209185, 209186), 1))
    assertNotNull(poll.validationError(listOf(209185, 209185), 1))
    assertNotNull(poll.validationError(listOf(209185), 1788704956))
  }

  @Test fun handlesHiddenResultsAndMalformedFields() {
    assertNull(NgaPollParser.parse("1", ""))
    assertNull(NgaPollParser.parse("1", "max_select~1"))
    val poll = NgaPollParser.parse("1", "42~A &amp; B~43~C~max_select~99~_42~x,0,0~incomplete")!!
    assertEquals("A & B", poll.options[0].label)
    assertNull(poll.totalVotes)
    assertEquals(2, poll.maxSelections)
    assertNull(poll.validationError(listOf(42, 43)))
    assertNotNull(poll.copy(isBet = true).validationError(listOf(42)))
  }

  @Test fun rejectsServerErrorsAndUnconfirmedResponses() {
    NgaPollParser.requireSuccessfulSubmission("""{"data":{"0":"操作成功"}}""")
    listOf("""{"error":{"0":"已经投票"}}""", """{"data":{"0":"未成功"}}""", "{}").forEach { raw ->
      assertTrue(runCatching { NgaPollParser.requireSuccessfulSubmission(raw) }.isFailure)
    }
  }
}
