package com.yanga.client.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import java.util.Locale

internal fun mediaTime(milliseconds: Int): String {
  val seconds = milliseconds.coerceAtLeast(0) / 1000
  return if (seconds >= 3600) String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
  else String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
}

@Composable
internal fun MediaSeekBar(position: Int, duration: Int, enabled: Boolean, label: String, onSeek: (Int) -> Unit) {
  var dragging by remember(duration) { mutableStateOf<Float?>(null) }
  val end = duration.coerceAtLeast(1).toFloat()
  Column {
    Slider(
      value = (dragging ?: position.toFloat()).coerceIn(0f, end),
      valueRange = 0f..end,
      enabled = enabled && duration > 0,
      onValueChange = { dragging = it },
      onValueChangeFinished = { dragging?.let { onSeek(it.toInt()) }; dragging = null },
      modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(mediaTime(dragging?.toInt() ?: position), style = MaterialTheme.typography.labelSmall)
      Text(if (duration > 0) mediaTime(duration) else "--:--", style = MaterialTheme.typography.labelSmall)
    }
  }
}
