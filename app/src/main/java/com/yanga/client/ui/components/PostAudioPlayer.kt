package com.yanga.client.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun PostAudioPlayer(
  url: String,
  label: String,
  modifier: Modifier = Modifier,
) {
  var isPlaying by remember(url) { mutableStateOf(false) }
  var isPreparing by remember(url) { mutableStateOf(false) }
  var errorMessage by remember(url) { mutableStateOf<String?>(null) }
  val player =
    remember(url) {
      MediaPlayer().apply {
        setOnPreparedListener {
          isPreparing = false
          errorMessage = null
          start()
          isPlaying = true
        }
        setOnCompletionListener {
          isPlaying = false
          seekTo(0)
        }
        setOnErrorListener { _, _, _ ->
          isPreparing = false
          isPlaying = false
          errorMessage = "无法播放"
          true
        }
      }
    }

  DisposableEffect(url) {
    onDispose {
      runCatching {
        if (player.isPlaying) player.stop()
        player.reset()
        player.release()
      }
    }
  }

  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .semantics { contentDescription = "Audio $label" },
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      IconButton(
        onClick = {
          errorMessage = null
          if (isPlaying) {
            player.pause()
            isPlaying = false
            return@IconButton
          }
          isPreparing = true
          runCatching {
            player.reset()
            player.setDataSource(url)
            player.prepareAsync()
          }.onFailure {
            isPreparing = false
            errorMessage = "无法播放"
          }
        },
        enabled = !isPreparing,
        modifier = Modifier.semantics { contentDescription = if (isPlaying) "Pause audio" else "Play audio" },
      ) {
        when {
          isPreparing -> {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
          }
          isPlaying -> {
            Icon(Icons.Filled.Pause, contentDescription = null)
          }
          else -> {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
          }
        }
      }
      Text(
        text = errorMessage ?: label,
        style = MaterialTheme.typography.bodyMedium,
        color =
          if (errorMessage != null) {
            MaterialTheme.colorScheme.error
          } else {
            MaterialTheme.colorScheme.onSurface
          },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
    }
  }
}
