package com.yanga.client.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanga.client.api.NgaPersonalTopicKind
import com.yanga.client.api.NgaPersonalTopicPage
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository

@Composable
internal fun PersonalTopicsScreen(kindName: String, repository: NgaReadOnlyRepository, session: LoginSessionData?, onBack: () -> Unit, onLogin: () -> Unit, navigate: (MainDestinationKey) -> Unit) {
  val kind = NgaPersonalTopicKind.entries.firstOrNull { it.name == kindName } ?: NgaPersonalTopicKind.Topics
  var page by rememberSaveable(kindName, session?.uid) { mutableIntStateOf(1) }
  var retry by remember { mutableIntStateOf(0) }
  var result by remember(kindName, page, session) { mutableStateOf<Result<NgaPersonalTopicPage>?>(null) }
  LaunchedEffect(kind, page, session, retry) {
    result = null
    if (session != null) result = repository.loadPersonalTopics(session, kind, page)
  }
  ProfilePageScaffold(title = kind.label, onBack = onBack, actions = {
    IconButton(enabled = session != null && result != null, onClick = { retry++ }) { Icon(Icons.Outlined.Refresh, "刷新本页") }
  }) { padding ->
    Column(Modifier.fillMaxSize().padding(padding)) {
      val current = result
      val data = current?.getOrNull()
      key(kind, page, session) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          when {
            session == null -> item { ProfileSectionCard {
              Icon(Icons.Outlined.Forum, null, tint = MaterialTheme.colorScheme.primary)
              Text("登录后查看${kind.label}", style = MaterialTheme.typography.titleLarge)
              Button(onClick = onLogin) { Text("登录 NGA") }
            } }
            current == null -> item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            current.isFailure -> item { ProfileSectionCard {
              Text("暂时无法加载", style = MaterialTheme.typography.titleLarge)
              Text(current.exceptionOrNull()?.message ?: "请稍后重试", color = MaterialTheme.colorScheme.onSurfaceVariant)
              FilledTonalButton(onClick = { retry++ }) { Text("重试") }
            } }
            data?.items?.isEmpty() == true -> item { ProfileSectionCard {
              Icon(Icons.Outlined.Forum, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
              Text(if (page == 1) "暂无内容" else "已到末页", style = MaterialTheme.typography.titleLarge)
              Text(if (page == 1) "${kind.label}会显示在这里" else "可以返回上一页继续浏览", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
          }
          items(data?.items.orEmpty()) { topic ->
            Card(
              onClick = { navigate(MainDestinationKey.Thread(ThreadDestination(id = topic.tid, title = topic.title, targetPostId = topic.pid))) },
              modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
              Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(topic.title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (topic.excerpt.isNotBlank()) Text(topic.excerpt, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (topic.pid != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Outlined.Forum, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                  Text("查看回复", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }
        }
      }
      if (session != null) Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          FilledTonalIconButton(enabled = page > 1, onClick = { page-- }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "上一页") }
          Text("第 $page 页", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
          FilledTonalIconButton(enabled = data?.hasNextPage == true, onClick = { page++ }) { Icon(Icons.AutoMirrored.Outlined.ArrowForward, "下一页") }
        }
      }
    }
  }
}
