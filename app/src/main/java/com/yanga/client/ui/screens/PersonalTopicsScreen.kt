package com.yanga.client.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanga.client.api.NgaPersonalTopicKind
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PersonalTopicsScreen(kindName: String, repository: NgaReadOnlyRepository, session: LoginSessionData?, onBack: () -> Unit, onLogin: () -> Unit, navigate: (MainDestinationKey) -> Unit, authorUid: String? = null, authorName: String = "") {
  val kind = NgaPersonalTopicKind.entries.firstOrNull { it.name == kindName } ?: NgaPersonalTopicKind.Topics
  val title = if (authorUid == null) kind.label else "${authorName.ifBlank { authorUid }}的主题"
  val topicsViewModel = viewModel<PersonalTopicsViewModel> { PersonalTopicsViewModel(repository) }
  val state by topicsViewModel.state.collectAsState()
  val canLoad = session != null || authorUid != null
  LaunchedEffect(kind, session, authorUid) {
    topicsViewModel.ensureLoaded(kind, session, authorUid)
  }
  ProfilePageScaffold(title = title, onBack = onBack) { padding ->
    key(kind, session, authorUid) {
      val listState = rememberLazyListState()
      LaunchedEffect(listState, state.page, state.hasNextPage, state.isRefreshing, state.isLoadingNext, state.nextPageError) {
        if (!state.hasNextPage || state.isRefreshing || state.isLoadingNext || state.nextPageError != null) return@LaunchedEffect
        snapshotFlow {
          val layout = listState.layoutInfo
          layout.totalItemsCount > 0 &&
            (layout.visibleItemsInfo.lastOrNull()?.index ?: -1) >= layout.totalItemsCount - 3
        }.distinctUntilChanged().collect { nearEnd ->
          if (nearEnd) topicsViewModel.loadNextPage()
        }
      }
      PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { if (canLoad) topicsViewModel.refresh() },
        modifier = Modifier.fillMaxSize().padding(padding),
      ) {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          when {
            !canLoad -> item(key = "login") { ProfileSectionCard {
              Icon(Icons.Outlined.Forum, null, tint = MaterialTheme.colorScheme.primary)
              Text("登录后查看${kind.label}", style = MaterialTheme.typography.titleLarge)
              Button(onClick = onLogin) { Text("登录 NGA") }
            } }
            state.isLoading -> item(key = "loading") {
              Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            state.error != null -> item(key = "error") { ProfileSectionCard {
              Text(if (state.page > 0) "刷新失败，已保留原有内容" else "暂时无法加载", style = MaterialTheme.typography.titleMedium)
              Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
              TextButton(onClick = topicsViewModel::refresh) { Text("重试") }
            } }
            state.page > 0 && state.items.isEmpty() -> item(key = "empty") { ProfileSectionCard {
              Icon(Icons.Outlined.Forum, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
              Text("暂无内容", style = MaterialTheme.typography.titleLarge)
              Text("${title}会显示在这里", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
          }
          items(state.items, key = { "topic:${it.tid}:${it.pid.orEmpty()}" }) { topic ->
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
          if (state.items.isNotEmpty()) item(key = "load-more") {
            Row(
              Modifier.fillMaxWidth().padding(vertical = 12.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              when {
                state.isLoadingNext -> {
                  CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                  Spacer(Modifier.width(12.dp))
                  Text("正在加载更多", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.nextPageError != null -> {
                  Text("加载失败", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  TextButton(onClick = topicsViewModel::loadNextPage) { Text("重试") }
                }
                !state.hasNextPage -> Text("已经看完所有内容", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    }
  }
}
