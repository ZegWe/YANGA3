package com.yanga.client.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanga.client.api.NgaDomains
import com.yanga.client.data.LoginSessionData
import com.yanga.client.theme.DarkModePreference
import com.yanga.client.theme.FixedThemeColor
import com.yanga.client.theme.ThemeColorPreference
import com.yanga.client.theme.ThemePreferences
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
  onThemeSettingsClick: () -> Unit = {},
  onAboutClick: () -> Unit = {},
  onAccountSettings: () -> Unit = {},
  onPersonalTopics: (String) -> Unit = {},
  onProfileRefresh: () -> Unit = {},
  onCheckIn: () -> Unit = {},
  onUserClick: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var showEndpointDialog by remember { mutableStateOf(false) }
  var showAccountSheet by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    PageHeader(
      title = "我的",
      subtitle = "账户设置与工具",
      modifier = Modifier.padding(horizontal = ProfileHorizontalPadding),
    )
    ProfileAccountCard(
      session = state.session,
      onLoginClick = {
        if (state.session is LoadableUiState.Content) {
          showAccountSheet = true
        } else {
          onLoginClick()
        }
      },
      modifier = Modifier.padding(horizontal = ProfileHorizontalPadding),
    )
    TextButton(onClick = onUserClick, modifier = Modifier.padding(horizontal = ProfileHorizontalPadding)) { Text("查看用户页面") }
    ProfileCounterGrid(
      counters = state.counters,
      modifier = Modifier.padding(horizontal = ProfileHorizontalPadding),
    )
    Row(modifier = Modifier.padding(horizontal = ProfileHorizontalPadding)) {
      TextButton(onClick = { onPersonalTopics("Topics") }) { Text("我的主题") }
      TextButton(onClick = { onPersonalTopics("Replies") }) { Text("我的回复") }
      TextButton(onClick = { onPersonalTopics("Favorites") }) { Text("收藏") }
    }
    TextButton(onClick = onProfileRefresh, modifier = Modifier.padding(horizontal = ProfileHorizontalPadding)) { Text("刷新资料与通知") }
    ProfileNotifications(state.notifications)
    SectionHeader(
      title = "账号",
      modifier = Modifier.padding(horizontal = ProfileHorizontalPadding),
    )
    AccountRows(onAccountSettings = onAccountSettings, state = state, onCheckIn = { if (loginSession == null) onLoginClick() else onCheckIn() })
    SectionHeader(
      title = "设置",
      modifier = Modifier.padding(horizontal = ProfileHorizontalPadding),
    )
    state.settingsRows.forEach { row ->
      SettingsRow(
        row = if (row.icon == "endpoint") row.copy(subtitle = state.forumEndpoint) else row,
        onClick =
          when (row.icon) {
            "endpoint" -> {
              { showEndpointDialog = true }
            }
            "theme" -> onThemeSettingsClick
            "about" -> onAboutClick
            else -> {
              {}
            }
          },
      )
    }
    if (state.session is LoadableUiState.Content) {
      Button(
        onClick = onLogout,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = ProfileHorizontalPadding),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
      ) {
        Text(text = "退出登录")
      }
    }
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

  if (showAccountSheet) {
    AccountSwitchSheet(
      session = state.session,
      onDismiss = { showAccountSheet = false },
      onAddAccount = {
        showAccountSheet = false
        onLoginClick()
      },
    )
  }
}

@Composable
internal fun ThemeSettingsScreen(
  preferences: ThemePreferences,
  onPreferencesChange: (ThemePreferences) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDarkTheme = preferences.darkMode.resolveDarkTheme(isSystemInDarkTheme())
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack) {
          Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
        }
        Text(text = "主题设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
      }
    }
    item { SectionHeader(title = "深色模式") }
    items(DarkModePreference.entries) { mode ->
      ThemeOptionRow(
        title = mode.label,
        selected = preferences.darkMode == mode,
        onClick = { onPreferencesChange(preferences.copy(darkMode = mode)) },
      )
    }
    item { SectionHeader(title = "主题色") }
    item {
      ThemeColorSwatchPicker(
        selectedColor = preferences.color,
        isDarkTheme = isDarkTheme,
        onSelectColor = { onPreferencesChange(preferences.copy(color = it)) },
        onDynamicColorChange = { enabled ->
          val color =
            if (enabled) {
              ThemeColorPreference.System
            } else {
              ThemeColorPreference.Fixed(FixedThemeColor.entries.first())
            }
          onPreferencesChange(preferences.copy(color = color))
        },
      )
    }
  }
}

@Composable
private fun ThemeOptionRow(
  title: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  trailing: @Composable () -> Unit = {},
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .selectable(
        selected = selected,
        role = Role.RadioButton,
        onClick = onClick,
      )
      .padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(selected = selected, onClick = null)
    Text(
      text = title,
      modifier = Modifier.weight(1f),
      style = MaterialTheme.typography.bodyLarge,
    )
    trailing()
  }
}

@Composable
private fun ThemeColorSwatchPicker(
  selectedColor: ThemeColorPreference,
  isDarkTheme: Boolean,
  onSelectColor: (ThemeColorPreference) -> Unit,
  onDynamicColorChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onDynamicColorChange(selectedColor != ThemeColorPreference.System) }
        .padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "动态取色",
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.bodyLarge,
      )
      Switch(
        checked = selectedColor == ThemeColorPreference.System,
        onCheckedChange = onDynamicColorChange,
      )
    }
    if (selectedColor != ThemeColorPreference.System) {
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(vertical = 4.dp),
      ) {
        lazyRowItems(FixedThemeColor.entries) { option ->
          val colorPreference = ThemeColorPreference.Fixed(option)
          ThemeColorSwatchCard(
            selected = selectedColor == colorPreference,
            contentDescription = "预设主题色 ${option.label}",
            onClick = { onSelectColor(colorPreference) },
          ) {
            MaterialThemeColorSwatch(colors = option.previewColors(isDark = isDarkTheme))
          }
        }
      }
    }
  }
}

@Composable
private fun ThemeColorSwatchCard(
  selected: Boolean,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Surface(
    modifier = modifier
      .selectable(
        selected = selected,
        role = Role.RadioButton,
        onClick = onClick,
      )
      .semantics { this.contentDescription = contentDescription },
    shape = MaterialTheme.shapes.medium,
    color =
      if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceContainerLow
      },
    tonalElevation = if (selected) 2.dp else 0.dp,
    border =
      androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        color =
          if (selected) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.outlineVariant
          },
      ),
  ) {
    Box(
      modifier = Modifier
        .size(72.dp)
        .padding(8.dp),
      contentAlignment = Alignment.Center,
    ) {
      content()
      if (selected) {
        Icon(
          imageVector = Icons.Outlined.Check,
          contentDescription = "已选主题色",
          modifier = Modifier.size(24.dp),
          tint = MaterialTheme.colorScheme.onPrimary,
        )
      }
    }
  }
}

@Composable
private fun MaterialThemeColorSwatch(
  colors: List<Color>,
  modifier: Modifier = Modifier,
) {
  val swatchSize = 48.dp
  val halfSwatchSize = swatchSize / 2
  Box(
    modifier = modifier
      .size(swatchSize)
      .clip(CircleShape),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(halfSwatchSize),
        color = colors[0],
      ) {}
      Row(modifier = Modifier.fillMaxWidth()) {
        Surface(
          modifier = Modifier
            .width(halfSwatchSize)
            .height(halfSwatchSize),
          color = colors[1],
        ) {}
        Surface(
          modifier = Modifier
            .width(halfSwatchSize)
            .height(halfSwatchSize),
          color = colors[2],
        ) {}
      }
    }
  }
}

private val ProfileHorizontalPadding = 20.dp

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
private fun ProfileAccountCard(
  session: LoadableUiState<LoginSessionData>,
  onLoginClick: () -> Unit,
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
            Text(text = session.value.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
          }
        }
      }
      when (session) {
        is LoadableUiState.Content ->
          IconButton(onClick = onLoginClick) {
            Icon(
              imageVector = Icons.Outlined.SwapHoriz,
              contentDescription = "切换账号",
            )
          }
        LoadableUiState.LoginRequired ->
          Button(onClick = onLoginClick) {
            Text(text = "登录")
          }
        is LoadableUiState.Empty,
        is LoadableUiState.Error,
        LoadableUiState.Loading -> Unit
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
    LoadableUiState.Loading -> ProfileCounterRow(counters = defaultProfileCounters(), modifier = modifier)
    is LoadableUiState.Content -> ProfileCounterRow(counters = counters.value.ifEmpty { defaultProfileCounters() }, modifier = modifier)
    is LoadableUiState.Empty -> ProfileCounterRow(counters = defaultProfileCounters(), modifier = modifier)
    is LoadableUiState.Error -> LoadableStateText(text = counters.message, modifier = modifier, isError = true)
    LoadableUiState.LoginRequired -> ProfileCounterRow(counters = defaultProfileCounters(), modifier = modifier)
  }
}

@Composable
private fun ProfileCounterRow(counters: List<SettingsPreview>, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    counters.forEach { counter ->
      ProfileCounter(
        label = counter.title,
        value = counter.badge ?: counter.subtitle,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

private fun defaultProfileCounters(): List<SettingsPreview> =
  listOf(
    SettingsPreview("topic", "主题", "--"),
    SettingsPreview("reply", "回复", "--"),
    SettingsPreview("notification", "通知", "--"),
  )

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
private fun AccountRows(onAccountSettings: () -> Unit, state: ProfileUiState, onCheckIn: () -> Unit, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxWidth()) {
    SettingsRow(
      row = SettingsPreview("account", "账号设置", "签名编辑与论坛个人中心"),
      onClick = onAccountSettings,
    )
    SettingsRow(
      row = SettingsPreview("check_in", "签到", state.checkInMessage ?: "点击进行每日签到"),
      onClick = { if (!state.checkInRunning) onCheckIn() },
    )
  }
}

@Composable
private fun SettingsRow(row: SettingsPreview, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .semantics { contentDescription = "${row.title}设置入口" }
      .padding(horizontal = ProfileHorizontalPadding, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SettingsIcon(icon = row.icon)
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
@OptIn(ExperimentalMaterial3Api::class)
private fun AccountSwitchSheet(
  session: LoadableUiState<LoginSessionData>,
  onDismiss: () -> Unit,
  onAddAccount: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (session is LoadableUiState.Content) {
        AccountSheetRow(session = session.value)
      }
      HorizontalDivider()
      TextButton(
        onClick = onAddAccount,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
        Text(text = "添加账号")
      }
    }
  }
}

@Composable
private fun AccountSheetRow(session: LoginSessionData, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = ProfileHorizontalPadding, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    UserAvatar(
      name = session.username,
      avatarUrl = session.avatarUrl,
      size = 36.dp,
    )
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(text = session.username, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      Text(
        text = "UID ${session.uid}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Icon(
      imageVector = Icons.Outlined.Check,
      contentDescription = "当前账号",
      tint = MaterialTheme.colorScheme.primary,
    )
  }
}

@Composable
private fun SettingsIcon(icon: String, modifier: Modifier = Modifier) {
  Icon(
    imageVector = icon.toSettingsImageVector(),
    contentDescription = null,
    modifier = modifier.padding(8.dp),
    tint = MaterialTheme.colorScheme.primary,
  )
}

private fun String.toSettingsImageVector(): ImageVector =
  when (this) {
    "account" -> Icons.Outlined.Person
    "check_in" -> Icons.Outlined.StarBorder
    "theme" -> Icons.Outlined.Palette
    "endpoint" -> Icons.Outlined.OpenInBrowser
    else -> Icons.Outlined.Settings
  }

@Composable
private fun ProfileNotifications(notifications: LoadableUiState<List<SettingsPreview>>) {
  Column(Modifier.padding(horizontal = ProfileHorizontalPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("通知", style = MaterialTheme.typography.titleMedium)
    when (notifications) {
      is LoadableUiState.Content -> if (notifications.value.isEmpty()) Text("暂无通知") else notifications.value.forEach { notice ->
        Text(notice.title, style = MaterialTheme.typography.titleSmall)
        Text(notice.subtitle, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider()
      }
      LoadableUiState.Loading -> Text("正在加载通知…")
      LoadableUiState.LoginRequired -> Text("登录后查看通知")
      is LoadableUiState.Error -> Text(notifications.message, color = MaterialTheme.colorScheme.error)
      is LoadableUiState.Empty -> Text(notifications.message)
    }
  }
}
