package com.yanga.client.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yanga.client.api.NgaUserProfile
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.ui.components.UserAvatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun UserProfileScreen(uid: String, repository: NgaReadOnlyRepository, session: LoginSessionData?, onBack: () -> Unit, onTopics: (String, String) -> Unit = { _, _ -> }) {
  var result by remember(uid, session) { mutableStateOf<Result<NgaUserProfile>?>(null) }
  var retry by remember { mutableIntStateOf(0) }
  LaunchedEffect(uid, session, retry) { result = null; result = repository.loadUser(session, uid) }
  ProfilePageScaffold(
    title = "用户资料", onBack = onBack,
    actions = { IconButton(onClick = { retry++ }, enabled = result != null) { Icon(Icons.Outlined.Refresh, "刷新资料") } },
  ) { padding ->
    Column(
      Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      val current = result
      if (current == null) Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      current?.fold(onSuccess = { user ->
        Card(
          modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
          Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
              UserAvatar(user.username, user.avatarUrl, Modifier.padding(6.dp), size = 80.dp)
            }
            Text(user.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)) {
              Text("UID ${user.uid}", Modifier.padding(horizontal = 16.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
            }
          }
        }
        ProfileSectionCard {
          Text("社区资料", style = MaterialTheme.typography.titleMedium)
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f).clickable { onTopics(user.uid, user.username) }.semantics { contentDescription = "查看用户发帖" }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(user.postCount?.toString() ?: "--", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
              Text("发帖数", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(user.registeredAt?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it * 1000)) } ?: "--", style = MaterialTheme.typography.titleLarge)
              Text("注册时间", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
        ProfileSectionCard {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.primary)
            Text("个性签名", style = MaterialTheme.typography.titleMedium)
          }
          Text(user.signature.ifBlank { "这个用户还没有留下签名" }, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }, onFailure = { error ->
        ProfileSectionCard {
          Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.error)
          Text("暂时无法加载资料", style = MaterialTheme.typography.titleLarge)
          Text(error.message ?: "请检查网络后重试", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          FilledTonalButton(onClick = { retry++ }) { Text("重试") }
        }
      })
    }
  }
}
