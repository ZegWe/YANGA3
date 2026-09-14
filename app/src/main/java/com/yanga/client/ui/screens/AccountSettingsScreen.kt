package com.yanga.client.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import com.yanga.client.ui.components.UserAvatar
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
  var avatarUrl by remember { mutableStateOf<String?>(null) }
  val snackbar = remember { SnackbarHostState() }
  var retry by remember { mutableIntStateOf(0) }
  val scope = rememberCoroutineScope()
  LaunchedEffect(retry) {
    loading = true
    if (session != null) repository.loadUser(session, session.uid).fold(onSuccess = {
      avatarUrl = it.avatarUrl
      signature = it.signature; original = it.signature; loaded = true; message = null
    }, onFailure = { message = it.message ?: "读取资料失败" })
    loading = false
  }
  ProfilePageScaffold(title = "账号设置", onBack = onBack, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
    Column(
      Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      if (session == null) {
        ProfileSectionCard {
          Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary)
          Text("登录后管理账号", style = MaterialTheme.typography.titleLarge)
          Text("编辑个性签名，管理个人资料与账号安全。", color = MaterialTheme.colorScheme.onSurfaceVariant)
          Button(onClick = onLogin) { Text("登录 NGA") }
        }
      } else {
        Card(
          modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
          Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            UserAvatar(session.username, avatarUrl, size = 56.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(session.username, style = MaterialTheme.typography.titleLarge)
              Text("UID ${session.uid}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
          }
        }
        ProfileSectionCard {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.primary)
            Text("个性签名", style = MaterialTheme.typography.titleMedium)
          }
          Text("在帖子和个人资料中展示你的签名。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
          if (loaded) {
            OutlinedTextField(
              value = signature, onValueChange = { signature = it; message = null },
              label = { Text("签名内容") }, supportingText = { Text("支持 NGA 标记，也可以留空清除签名") },
              enabled = !saving, minLines = 4, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
            )
            if (signature.isNotBlank()) {
              Text("签名预览", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
              Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                SignatureContent(signature, Modifier.padding(16.dp))
              }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
              Button(enabled = !saving && signature != original, onClick = {
                saving = true; message = null
                scope.launch {
                  val result = repository.saveSignature(session, signature)
                  result.fold(onSuccess = { original = signature }, onFailure = { message = it.message ?: "保存失败，请重试" })
                  saving = false
                  if (result.isSuccess) snackbar.showSnackbar("签名已保存")
                }
              }) {
                if (saving) {
                  CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                  Spacer(Modifier.width(8.dp))
                }
                Text(if (saving) "保存中…" else "保存签名")
              }
            }
          } else if (!loading) {
            FilledTonalButton(onClick = { retry++ }) { Text("重新加载") }
          }
          message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        Card(
          onClick = onForumSettings, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
          ListItem(
            headlineContent = { Text("头像与账号安全") },
            supportingContent = { Text("前往论坛个人中心管理") },
            leadingContent = { Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, "打开论坛个人中心") },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
          )
        }
      }
    }
  }
}
