package com.yanga.client.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal interface SystemQrScanner {
  fun start(onResult: (String) -> Unit, onCancelled: () -> Unit, onUnavailable: () -> Unit)
}

internal class GoogleQrScanner(private val context: Context) : SystemQrScanner {
  override fun start(onResult: (String) -> Unit, onCancelled: () -> Unit, onUnavailable: () -> Unit) {
    if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
      onUnavailable()
      return
    }
    val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build()
    GmsBarcodeScanning.getClient(context, options).startScan()
      .addOnSuccessListener { result -> result.rawValue?.takeIf(String::isNotBlank)?.let(onResult) ?: onUnavailable() }
      .addOnCanceledListener(onCancelled)
      .addOnFailureListener { onUnavailable() }
  }
}

internal sealed interface QrScanState {
  data object Opening : QrScanState
  data object Local : QrScanState
  data class Result(val text: String) : QrScanState
  data object Cancelled : QrScanState
}

internal class QrScannerViewModel : ViewModel() {
  private val mutableState = MutableStateFlow<QrScanState>(QrScanState.Opening)
  val state = mutableState.asStateFlow()
  private var started = false
  private var cleared = false

  fun start(scanner: SystemQrScanner) {
    if (started) return
    started = true
    fun fallback() { if (!cleared && mutableState.value == QrScanState.Opening) mutableState.value = QrScanState.Local }
    runCatching {
      scanner.start(
        onResult = { if (mutableState.value == QrScanState.Opening) complete(it) },
        onCancelled = { if (mutableState.value == QrScanState.Opening) cancel() },
        onUnavailable = ::fallback,
      )
    }.onFailure { fallback() }
  }

  fun complete(text: String) {
    if (!cleared && (mutableState.value == QrScanState.Opening || mutableState.value == QrScanState.Local)) {
      mutableState.value = QrScanState.Result(text)
    }
  }
  fun cancel() { if (!cleared) mutableState.value = QrScanState.Cancelled }
  override fun onCleared() { cleared = true }
}
