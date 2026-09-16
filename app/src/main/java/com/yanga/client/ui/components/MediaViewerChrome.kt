package com.yanga.client.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun MediaViewerTheme(content: @Composable () -> Unit) {
  val colors = MaterialTheme.colorScheme
  MaterialTheme(colorScheme = darkColorScheme(
    primary = colors.inversePrimary,
    secondaryContainer = colors.secondaryContainer,
    onSecondaryContainer = colors.onSecondaryContainer,
  ), content = content)
}

@Composable
internal fun MediaViewerHeader(title: String, subtitle: String, closeDescription: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
  Surface(modifier.statusBarsPadding().padding(16.dp), shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .96f)) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      FilledTonalIconButton(onClick = onClose) { Icon(Icons.Default.Close, closeDescription) }
      Column(Modifier.weight(1f).padding(end = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}
