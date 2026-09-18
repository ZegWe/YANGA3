package com.yanga.client.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class QrScannerViewModelTest {
  private class Scanner : SystemQrScanner {
    var starts = 0
    lateinit var result: (String) -> Unit
    lateinit var cancelled: () -> Unit
    lateinit var unavailable: () -> Unit
    override fun start(onResult: (String) -> Unit, onCancelled: () -> Unit, onUnavailable: () -> Unit) {
      starts++
      result = onResult
      cancelled = onCancelled
      unavailable = onUnavailable
    }
  }

  @Test fun successfulSystemScanReturnsResultAndStartsOnlyOnce() {
    val model = QrScannerViewModel()
    val scanner = Scanner()
    model.start(scanner)
    model.start(scanner)
    scanner.result("qr")
    assertEquals(1, scanner.starts)
    assertEquals(QrScanState.Result("qr"), model.state.value)
  }

  @Test fun unavailableFallsBackAndIgnoresLateSystemCallbacks() {
    val model = QrScannerViewModel()
    val scanner = Scanner()
    model.start(scanner)
    scanner.unavailable()
    scanner.result("late")
    scanner.cancelled()
    assertEquals(QrScanState.Local, model.state.value)
    model.complete("local")
    assertEquals(QrScanState.Result("local"), model.state.value)
  }

  @Test fun cancellationDoesNotOpenFallback() {
    val model = QrScannerViewModel()
    val scanner = Scanner()
    model.start(scanner)
    scanner.cancelled()
    scanner.unavailable()
    scanner.result("late")
    assertEquals(QrScanState.Cancelled, model.state.value)
  }

  @Test fun synchronousLaunchFailureOpensFallback() {
    val model = QrScannerViewModel()
    model.start(object : SystemQrScanner {
      override fun start(onResult: (String) -> Unit, onCancelled: () -> Unit, onUnavailable: () -> Unit) {
        error("Scanner unavailable")
      }
    })
    assertEquals(QrScanState.Local, model.state.value)
  }
}
