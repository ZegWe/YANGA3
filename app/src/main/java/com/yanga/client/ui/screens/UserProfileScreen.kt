package com.yanga.client.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanga.client.api.NgaUserProfile
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.ui.components.UserAvatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun UserProfileScreen(uid: String, repository: NgaReadOnlyRepository, session: LoginSessionData?, onBack: () -> Unit) {
  var result by remember(uid, session) { mutableStateOf<Result<NgaUserProfile>?>(null) }
  var retry by remember { mutableIntStateOf(0) }
  LaunchedEffect(uid, session, retry) { result = null; result = repository.loadUser(session, uid) }
  Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    TextButton(onClick = onBack) { Text("返回") }
    Text("用户资料", style = MaterialTheme.typography.headlineSmall)
    val current = result
    if (current == null) CircularProgressIndicator()
    current?.fold(onSuccess = { user ->
      UserAvatar(user.username, user.avatarUrl, size = 64.dp)
      Text(user.username, style = MaterialTheme.typography.titleLarge)
      Text("UID ${user.uid}")
      Text("发帖数：${user.postCount ?: "--"}")
      Text("注册时间：" + (user.registeredAt?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it * 1000)) } ?: "--"))
      HorizontalDivider()
      Text("签名", style = MaterialTheme.typography.titleMedium)
      Text(user.signature.ifBlank { "暂无签名" })
    }, onFailure = { error ->
      Text(error.message ?: "加载失败", color = MaterialTheme.colorScheme.error)
      Button(onClick = { retry++ }) { Text("重试") }
    })
  }
}
