package com.yanga.client.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import kotlinx.coroutines.launch

@Composable
internal fun AccountSettingsScreen(repository: NgaReadOnlyRepository, session: LoginSessionData?, onBack: () -> Unit, onLogin: () -> Unit, onForumSettings: () -> Unit) {
  // Discard editor and requests when the account changes.
  key(session) { AccountSettingsEditor(repository, session, onBack, onLogin, onForumSettings) }
}

@Composable
private fun AccountSettingsEditor(repository: NgaReadOnlyRepository, session: LoginSessionData?, onBack: () -> Unit, onLogin: () -> Unit, onForumSettings: () -> Unit) {
  var signature by remember { mutableStateOf("") }
  var original by remember { mutableStateOf("") }
  var loaded by remember { mutableStateOf(false) }
  var loading by remember { mutableStateOf(true) }
  var saving by remember { mutableStateOf(false) }
  var message by remember { mutableStateOf<String?>(null) }
  var retry by remember { mutableIntStateOf(0) }
  val scope = rememberCoroutineScope()
  LaunchedEffect(retry) {
    loading = true
    if (session != null) repository.loadUser(session, session.uid).fold(onSuccess = {
      signature = it.signature; original = it.signature; loaded = true; message = null
    }, onFailure = { message = it.message ?: "读取资料失败" })
    loading = false
  }
  Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    TextButton(onClick = onBack) { Text("返回") }
    Text("账号设置", style = MaterialTheme.typography.headlineSmall)
    if (session == null) {
      Button(onClick = onLogin) { Text("登录 NGA") }
    } else {
      Text("${session.username} · UID ${session.uid}")
      if (loading) CircularProgressIndicator()
      if (loaded) {
        OutlinedTextField(value = signature, onValueChange = { signature = it; message = null }, label = { Text("签名（支持 NGA 标记）") }, enabled = !saving, minLines = 4, modifier = Modifier.fillMaxWidth())
        Button(enabled = !saving && signature != original, onClick = {
          saving = true; message = null
          scope.launch {
            repository.saveSignature(session, signature).fold(onSuccess = {
              original = signature; message = "签名已保存"
            }, onFailure = { message = it.message ?: "保存失败，请重试" })
            saving = false
          }
        }) { Text(if (saving) "保存中…" else "保存签名") }
      } else if (!loading) {
        Button(onClick = { retry++ }) { Text("重新加载") }
      }
      message?.let { Text(it) }
      HorizontalDivider()
      Text("头像与账号安全", style = MaterialTheme.typography.titleMedium)
      TextButton(onClick = onForumSettings) { Text("打开论坛个人中心") }
    }
  }
}
