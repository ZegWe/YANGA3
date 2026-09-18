package com.yanga.client.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplyComposer(
  title: String,
  model: ReplyViewModel,
  loggedIn: Boolean,
  onSend: () -> Unit,
  onWeb: () -> Unit,
) {
  val content by model.content.collectAsState()
  val target by model.target.collectAsState()
  val sending by model.sending.collectAsState()
  val error by model.error.collectAsState()
  Dialog(onDismissRequest = model::close, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !sending, dismissOnClickOutside = false)) {
    Scaffold(
      modifier = Modifier.fillMaxSize().imePadding(),
      topBar = {
        TopAppBar(title = { Text("回帖") }, navigationIcon = {
          IconButton(onClick = model::close, enabled = !sending) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回并保留草稿")
          }
        }, actions = {
          TextButton(onClick = onWeb, enabled = !sending) { Text("网页回帖") }
          Button(onClick = onSend, enabled = loggedIn && content.isNotBlank() && !sending) {
            if (sending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("发送")
          }
        })
      },
    ) { padding ->
      Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
        Text(target, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        if (!loggedIn) Text("请先登录后再发送，已输入的内容会保留。", color = MaterialTheme.colorScheme.error)
        OutlinedTextField(
          value = content, onValueChange = model::edit, enabled = !sending,
          modifier = Modifier.fillMaxWidth().weight(1f),
          label = { Text("回复内容") }, placeholder = { Text("分享你的想法…") },
          shape = MaterialTheme.shapes.large,
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("支持 NGA 标记语法 · 返回自动保留草稿", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}
