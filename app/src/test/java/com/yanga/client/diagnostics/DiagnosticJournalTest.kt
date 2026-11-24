package com.yanga.client.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DiagnosticJournalTest {
  @get:Rule val temp = TemporaryFolder()

  @Test fun fatalReportPreservesStackAndContextButNotExceptionMessage() {
    val journal = DiagnosticJournal(temp.newFolder())
    journal.append("render_issue_mark page=2")
    val error = IllegalStateException("Cookie=never-export-me", IllegalArgumentException("private body"))
    journal.crash(error, mainThread = true)
    // Read from a new instance to exercise persistence, not the original in-memory queue.
    val recovered = DiagnosticJournal(temp.root.listFiles()!!.single()).snapshot()
    assertTrue(recovered.contains("fatal java_or_kotlin main_thread=true"))
    assertTrue(recovered.contains("IllegalStateException"))
    assertTrue(recovered.contains("IllegalArgumentException"))
    assertTrue(recovered.contains("fatalReportPreservesStackAndContextButNotExceptionMessage"))
    assertTrue(recovered.contains("render_issue_mark page=2"))
    assertFalse(recovered.contains("never-export-me"))
    assertFalse(recovered.contains("private body"))
  }

  @Test fun textMetricsAndTagsCompareWithoutRevealingInput() {
    val text = "中文\uFFFD\u0001"
    assertEquals("chars=4 replacement=1 controls=1 private_use=0", Diagnostics.textStats(text))
    val first = Diagnostics.fingerprint(text)
    assertTrue(first.matches(Regex("[0-9a-f]{16}")))
    assertEquals(first, Diagnostics.fingerprint(text))
    assertNotEquals(first, Diagnostics.fingerprint("中文"))
  }

  @Test fun normalEventsPersistAndRotateAtTheSizeLimit() {
    val dir = temp.newFolder()
    dir.resolve("events.log").writeText("x".repeat(256 * 1024))
    val journal = DiagnosticJournal(dir)
    journal.append("thread_load result=ok")
    assertTrue(journal.snapshot().contains("thread_load result=ok"))
    assertTrue(dir.resolve("events-previous.log").isFile)
    assertTrue(dir.resolve("events.log").length() < 200)
  }
}
