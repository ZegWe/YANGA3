package com.yanga.client.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
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
  Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    TextButton(onClick = onBack) { Text("返回") }
    Text(kind.label, style = MaterialTheme.typography.headlineSmall)
    if (session == null) {
      Button(onClick = onLogin) { Text("登录 NGA") }
    } else {
      val current = result
      if (current == null) CircularProgressIndicator()
      current?.exceptionOrNull()?.let { error ->
        Text(error.message ?: "加载失败", color = MaterialTheme.colorScheme.error)
        Button(onClick = { retry++ }) { Text("重试") }
      }
      val data = current?.getOrNull()
      LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (data?.items?.isEmpty() == true) item { Text(if (page == 1) "暂无内容" else "已到末页") }
        items(data?.items.orEmpty()) { topic ->
          Column(Modifier.fillMaxWidth().clickable {
            navigate(MainDestinationKey.Thread(ThreadDestination(id = topic.tid, title = topic.title, targetPostId = topic.pid)))
          }.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(topic.title, style = MaterialTheme.typography.titleMedium)
            if (topic.excerpt.isNotBlank()) Text(topic.excerpt, maxLines = 3)
            HorizontalDivider()
          }
        }
      }
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(enabled = page > 1, onClick = { page-- }) { Text("上一页") }
        Text("第 $page 页")
        TextButton(enabled = data?.hasNextPage == true, onClick = { page++ }) { Text("下一页") }
      }
      TextButton(enabled = current != null, onClick = { retry++ }) { Text("刷新本页") }
    }
  }
}
