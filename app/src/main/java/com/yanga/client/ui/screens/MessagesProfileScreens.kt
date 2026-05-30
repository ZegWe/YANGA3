package com.yanga.client.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanga.client.api.NgaDomains
import com.yanga.client.data.LoginSessionData
import com.yanga.client.ui.components.UserAvatar

@Composable
internal fun MessagesScreen(
  loginSession: LoginSessionUiState? = null,
  state: MessagesUiState = MessagesUiState(),
  onLoginClick: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      PageHeader(
        title = "Messages",
        subtitle = "Private messages",
      )
    }
    item { SearchPill(text = "Search private messages") }
    item {
      FilterChipRow(
        labels = listOf("All", "Unread", "Sent", "Blocked"),
        selectedIndex = 0,
      )
    }
    item {
      ActionChipRow(labels = listOf("Write private message", "Block list"))
    }
    item { SectionHeader(title = "Conversations", trailing = "New") }
    when (val messages = state.messages) {
      LoadableUiState.Loading -> item { LoadableStateText(text = "Loading private messages") }
      is LoadableUiState.Content ->
        if (messages.value.isEmpty()) {
          item { LoadableStateText(text = "No private messages") }
        } else {
          items(messages.value) { message ->
            MessageThreadRow(message = message)
          }
        }
      is LoadableUiState.Empty -> item { LoadableStateText(text = messages.message) }
      is LoadableUiState.Error -> item { LoadableStateText(text = messages.message, isError = true) }
      LoadableUiState.LoginRequired ->
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LoadableStateText(text = "Sign in to load private messages")
            if (loginSession == null) {
              Button(onClick = onLoginClick) {
                Text(text = "登录 NGA")
              }
            }
          }
        }
    }
  }
}

@Composable
internal fun ProfileScreen(
  loginSession: LoginSessionUiState?,
  state: ProfileUiState = ProfileUiState(),
  onLoginClick: () -> Unit,
  onLogout: () -> Unit,
  onEndpointChange: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var showEndpointDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    PageHeader(
      title = "Profile",
      subtitle = "Account, notifications, and settings",
    )
    ProfileAccountCard(
      session = state.session,
      onLoginClick = onLoginClick,
      onLogout = onLogout,
    )
    ProfileCounterGrid(counters = state.counters)
    SectionHeader(title = "Notification center")
    SettingsRowsContent(state = state.notifications)
    SectionHeader(title = "Settings")
    SettingsRow(
      row = SettingsPreview("端", "论坛端点", state.forumEndpoint),
      onClick = { showEndpointDialog = true }
    )
    state.settingsRows.forEach { row ->
      SettingsRow(row = row)
    }
    SettingsRow(
      row = SettingsPreview("黑", "Block list", "Private message blocked users"),
    )
  }

  if (showEndpointDialog) {
    EndpointSelectionDialog(
      currentEndpoint = state.forumEndpoint,
      onDismiss = { showEndpointDialog = false },
      onSelect = {
        onEndpointChange(it)
        showEndpointDialog = false
      }
    )
  }
}

@Composable
private fun EndpointSelectionDialog(
  currentEndpoint: String,
  onDismiss: () -> Unit,
  onSelect: (String) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("选择论坛端点") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NgaDomains.supported.forEach { domain ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelect(domain) }
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            RadioButton(
              selected = domain == currentEndpoint,
              onClick = null
            )
            Text(text = domain, style = MaterialTheme.typography.bodyLarge)
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("关闭")
      }
    }
  )
}

@Composable
private fun LoadableStateText(
  text: String,
  modifier: Modifier = Modifier,
  isError: Boolean = false,
) {
  Text(
    text = text,
    modifier = modifier.fillMaxWidth(),
    style = MaterialTheme.typography.bodyMedium,
    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun MessageThreadRow(message: MessagePreview, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    shape = MaterialTheme.shapes.large,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      RoundMarker(text = message.contact)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = message.contact,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = message.time,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = message.preview,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      message.badge?.let { Badge { Text(it) } }
    }
  }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ProfileAccountCard(
  session: LoadableUiState<LoginSessionData>,
  onLoginClick: () -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier,
) {
  TonalCard(modifier = modifier) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val markerText = if (session is LoadableUiState.Content) session.value.username else "未"
      if (session is LoadableUiState.Content) {
        UserAvatar(
          name = session.value.username,
          avatarUrl = session.value.avatarUrl,
          size = 42.dp,
        )
      } else {
        RoundMarker(text = markerText)
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (session) {
          LoadableUiState.Loading -> {
            Text(text = "Loading profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
              text = "Loading account, notifications, and settings.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          is LoadableUiState.Content -> {
            Text(text = "已登录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = session.value.username, style = MaterialTheme.typography.bodyLarge)
            Text(
              text = "UID ${session.value.uid}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          is LoadableUiState.Empty -> {
            Text(text = session.message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
              text = "No account profile is available.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          is LoadableUiState.Error -> {
            Text(
              text = session.message,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.error,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "Profile data could not be loaded.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          LoadableUiState.LoginRequired -> {
            Text(text = "当前未登录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
              text = "Sign in to load profile",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
    if (session !is LoadableUiState.Content) {
      Button(onClick = onLoginClick) {
        Text(text = "登录 NGA")
      }
    } else {
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(onClick = onLoginClick) {
          Text(text = "Switch account")
        }
        OutlinedButton(onClick = onLogout) {
          Text(text = "退出登录")
        }
        TextButton(onClick = {}) {
          Text(text = "Check in")
        }
      }
    }
  }
}

@Composable
private fun ProfileCounterGrid(
  counters: LoadableUiState<List<SettingsPreview>>,
  modifier: Modifier = Modifier,
) {
  when (counters) {
    LoadableUiState.Loading -> LoadableStateText(text = "Loading profile counters", modifier = modifier)
    is LoadableUiState.Content ->
      if (counters.value.isEmpty()) {
        LoadableStateText(text = "No profile counters", modifier = modifier)
      } else {
        Row(
          modifier = modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          counters.value.forEach { counter ->
            ProfileCounter(
              label = counter.title,
              value = counter.badge ?: counter.subtitle,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    is LoadableUiState.Empty -> LoadableStateText(text = counters.message, modifier = modifier)
    is LoadableUiState.Error -> LoadableStateText(text = counters.message, modifier = modifier, isError = true)
    LoadableUiState.LoginRequired -> LoadableStateText(text = "Sign in to load profile counters", modifier = modifier)
  }
}

@Composable
private fun ProfileCounter(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = MaterialTheme.shapes.large,
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun SettingsRow(row: SettingsPreview, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Marker(text = row.icon)
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(text = row.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      Text(
        text = row.subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
    row.badge?.let { Badge { Text(it) } }
  }
}

@Composable
private fun SettingsRowsContent(
  state: LoadableUiState<List<SettingsPreview>>,
  modifier: Modifier = Modifier,
) {
  when (state) {
    LoadableUiState.Loading -> LoadableStateText(text = "Loading notifications", modifier = modifier)
    is LoadableUiState.Content ->
      if (state.value.isEmpty()) {
        LoadableStateText(text = "No notifications", modifier = modifier)
      } else {
        Column(modifier = modifier.fillMaxWidth()) {
          state.value.forEach { row ->
            SettingsRow(row = row)
          }
        }
      }
    is LoadableUiState.Empty -> LoadableStateText(text = state.message, modifier = modifier)
    is LoadableUiState.Error -> LoadableStateText(text = state.message, modifier = modifier, isError = true)
    LoadableUiState.LoginRequired -> LoadableStateText(text = "Sign in to load notifications", modifier = modifier)
  }
}








