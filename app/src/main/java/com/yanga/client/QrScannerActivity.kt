package com.yanga.client

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanga.client.ui.*

class QrScannerActivity : YangaComposeActivity() {
  private val model by viewModels<QrScannerViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    model.start(GoogleQrScanner(applicationContext))
  }

  @OptIn(ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  override fun Content() {
    val state by model.state.collectAsState()
    BackHandler { model.cancel() }
    LaunchedEffect(state) {
      when (val current = state) {
        is QrScanState.Result -> {
          setResult(Activity.RESULT_OK, Intent().putExtra(RESULT_TEXT, current.text))
          finish()
        }
        QrScanState.Cancelled -> finish()
        else -> Unit
      }
    }
    if (state == QrScanState.Local) {
      LocalQrScannerScreen(onResult = model::complete, onBack = model::cancel)
    } else {
      ProfilePageScaffold("扫一扫", model::cancel) { padding ->
        Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
          LoadingIndicator(Modifier.size(64.dp))
          Text("正在打开扫码工具", style = MaterialTheme.typography.bodyLarge)
        }
      }
    }
  }

  companion object { const val RESULT_TEXT = "qr_result" }
}
