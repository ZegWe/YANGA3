package com.yanga.client.diagnostics

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DiagnosticsPanel() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var snapshot by remember { mutableStateOf<String?>(null) }
  var busy by remember { mutableStateOf(false) }
  var notice by remember { mutableStateOf<String?>(null) }
  // Keep the exact previewed bytes across rotation while Android's picker is open.
  var pendingFile by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
  val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
    val name = pendingFile
    pendingFile = null
    if (uri != null && name != null) scope.launch {
      val ok = withContext(Dispatchers.IO) {
        runCatching {
          val file = File(File(context.cacheDir, "diagnostic-exports"), name)
          checkNotNull(context.contentResolver.openOutputStream(uri, "w")).use { output ->
            file.inputStream().use { it.copyTo(output) }
          }
        }.isSuccess
      }
      notice = if (ok) "日志已导出" else "导出失败，请重新预览后再试"
    }
  }

  Card {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("诊断日志", style = MaterialTheme.typography.titleMedium)
      Text("日志只保存在本机，不会自动上传。不记录 Cookie、账号、帖子正文或完整网址。可以先预览，再保存文件，或选择接收方分享。")
      OutlinedButton(enabled = !busy, onClick = {
        scope.launch {
          busy = true
          try {
            snapshot = withContext(Dispatchers.IO) { runCatching { Diagnostics.snapshot() }.getOrNull() }
            notice = if (snapshot == null) "无法读取诊断日志" else null
          } finally {
            busy = false
          }
        }
      }) { Text(if (busy) "读取中…" else "预览日志") }
      notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
  }

  val report = snapshot
  if (report != null) {
    fun prepare(send: Boolean) {
      busy = true
      scope.launch {
        try {
          val file = withContext(Dispatchers.IO) {
            runCatching {
              val dir = File(context.cacheDir, "diagnostic-exports").apply { mkdirs() }
              // Bound the number retained, while leaving recent shares available for slow readers.
              dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(4)?.forEach { it.delete() }
              File.createTempFile("yanga-diagnostics-", ".txt", dir).apply { writeText(report) }
            }.getOrNull()
          }
          if (file == null) {
            notice = "无法生成日志文件"
          } else if (send) {
            runCatching {
              val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
              val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("Yanga diagnostics", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
              }
              context.startActivity(Intent.createChooser(intent, "选择日志接收方"))
            }.onFailure { notice = "无法打开分享菜单" }
          } else {
            pendingFile = file.name
            runCatching { export.launch("yanga-diagnostics.txt") }.onFailure {
              pendingFile = null
              notice = "无法打开文件保存窗口"
            }
          }
          snapshot = null
        } finally {
          busy = false
        }
      }
    }

    AlertDialog(
      onDismissRequest = { if (!busy) snapshot = null },
      title = { Text("诊断日志预览") },
      text = {
        Column(Modifier.heightIn(max = 370.dp)) {
          Text("分享会打开系统菜单，由你选择是否发送，以及发给谁。")
          // Render in bounded chunks so a multi-session log is not one enormous text layout.
          val chunks = remember(report) { report.lineSequence().chunked(40).map { it.joinToString("\n") }.toList() }
          androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f, fill = false).padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(chunks.size) { index ->
              SelectionContainer {
                Text(chunks[index], fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      },
      confirmButton = { TextButton(enabled = !busy, onClick = { prepare(true) }) { Text("分享 / 上传") } },
      dismissButton = {
        Row {
          TextButton(enabled = !busy, onClick = { snapshot = null }) { Text("关闭") }
          TextButton(enabled = !busy, onClick = { prepare(false) }) { Text("保存文件") }
        }
      },
    )
  }
}
