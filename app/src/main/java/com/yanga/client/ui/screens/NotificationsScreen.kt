package com.yanga.client.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanga.client.api.NgaNotificationSummary
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun NotificationsScreen(repository: NgaReadOnlyRepository, session: LoginSessionData?, onBack: () -> Unit, onLogin: () -> Unit) {
  var result by remember(session) { mutableStateOf<Result<List<NgaNotificationSummary>>?>(null) }
  var refresh by remember { mutableIntStateOf(0) }
  LaunchedEffect(session, refresh) {
    result = null
    if (session != null) result = repository.loadNotifications(session)
  }
  ProfilePageScaffold(title = "通知", onBack = onBack, actions = {
    IconButton(onClick = { refresh++ }, enabled = session != null && result != null) { Icon(Icons.Outlined.Refresh, "刷新通知") }
  }) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      when {
        session == null -> item { ProfileSectionCard {
          Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.primary)
          Text("登录后查看通知", style = MaterialTheme.typography.titleLarge)
          Button(onClick = onLogin) { Text("登录 NGA") }
        } }
        result == null -> item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        result?.isFailure == true -> item { ProfileSectionCard {
          Text("通知加载失败", style = MaterialTheme.typography.titleLarge)
          Text(result?.exceptionOrNull()?.message ?: "请稍后重试", color = MaterialTheme.colorScheme.onSurfaceVariant)
          FilledTonalButton(onClick = { refresh++ }) { Text("重试") }
        } }
        result?.getOrNull().isNullOrEmpty() -> item { ProfileSectionCard {
          Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
          Text("暂无通知", style = MaterialTheme.typography.titleLarge)
          Text("新的回复与提醒会显示在这里", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        else -> items(result?.getOrNull().orEmpty()) { notice ->
          ProfileSectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.primary)
              Text(notice.title.ifBlank { "论坛通知" }, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
              if (notice.unreadCount > 0) Badge { Text(notice.unreadCount.toString()) }
            }
            if (notice.preview.isNotBlank()) Text(notice.preview, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            notice.createdAt?.let { Text(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it * 1000)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
          }
        }
      }
    }
  }
}
