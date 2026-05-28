package com.yanga.client.ui.main

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
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun MessagesScreen(modifier: Modifier = Modifier) {
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
    items(privateMessages) { message ->
      MessageThreadRow(message = message)
    }
  }
}

@Composable
internal fun ProfileScreen(
  loginSession: LoginSessionUiState?,
  onLoginClick: () -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier,
) {
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
      loginSession = loginSession,
      onLoginClick = onLoginClick,
      onLogout = onLogout,
    )
    ProfileCounterGrid()
    SectionHeader(title = "Notification center")
    SettingsRow(
      row = SettingsPreview("通", "Notifications", "Reply alerts and favorite topic updates", "12"),
    )
    SectionHeader(title = "Settings")
    settingsRows.forEach { row ->
      SettingsRow(row = row)
    }
    SettingsRow(
      row = SettingsPreview("黑", "Block list", "Private message blocked users"),
    )
  }
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
  loginSession: LoginSessionUiState?,
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
      RoundMarker(text = loginSession?.username ?: "未")
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (loginSession == null) {
          Text(text = "当前未登录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Text(
            text = "登录后同步私信、收藏和通知。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          Text(text = "已登录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Text(text = loginSession.username, style = MaterialTheme.typography.bodyLarge)
          Text(
            text = "UID ${loginSession.uid}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
    if (loginSession == null) {
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
private fun ProfileCounterGrid(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    ProfileCounter(
      label = "Favorite topics",
      value = "36",
      modifier = Modifier.weight(1f),
    )
    ProfileCounter(
      label = "Subscribed boards",
      value = "12",
      modifier = Modifier.weight(1f),
    )
    ProfileCounter(
      label = "New notifications",
      value = "12",
      modifier = Modifier.weight(1f),
    )
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
private fun SettingsRow(row: SettingsPreview, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .fillMaxWidth()
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
