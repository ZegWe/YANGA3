package com.yanga.client.ui.components

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

@Composable
fun PostAudioPlayer(url: String, label: String, modifier: Modifier = Modifier, onOpenLink: (String) -> Unit = {}) {
  var isPlaying by remember(url) { mutableStateOf(false) }
  var isPreparing by remember(url) { mutableStateOf(false) }
  var prepared by remember(url) { mutableStateOf(false) }
  var error by remember(url) { mutableStateOf(false) }
  var player by remember(url) { mutableStateOf<MediaPlayer?>(null) }
  var duration by remember(url) { mutableIntStateOf(0) }
  var position by remember(url) { mutableIntStateOf(0) }
  var seeking by remember(url) { mutableStateOf(false) }
  val context = LocalContext.current
  val owner = LocalLifecycleOwner.current
  LaunchedEffect(url, prepared, isPlaying) {
    while (prepared) {
      if (!seeking) position = runCatching { player?.currentPosition ?: 0 }.getOrDefault(position)
      delay(250)
    }
  }
  DisposableEffect(url, owner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE) {
        if (isPlaying) player?.pause()
        isPlaying = false
      }
    }
    owner.lifecycle.addObserver(observer)
    onDispose {
      owner.lifecycle.removeObserver(observer)
      player?.release()
      player = null
      prepared = false
      isPlaying = false
      isPreparing = false
    }
  }
  Surface(modifier.fillMaxWidth().semantics { contentDescription = "Audio $label" },
    color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        FilledIconButton(
          enabled = !isPreparing,
          modifier = Modifier.size(56.dp).semantics { contentDescription = if (isPlaying) "Pause audio" else "Play audio" },
          onClick = {
            error = false
            if (isPlaying) {
              player?.pause()
              isPlaying = false
            } else if (prepared) {
              player?.start()
              isPlaying = true
            } else {
              isPreparing = true
              player?.release()
              runCatching {
                player = MediaPlayer().apply {
                  setOnPreparedListener {
                    duration = it.duration.coerceAtLeast(0)
                    position = 0
                    prepared = true
                    isPreparing = false
                    if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                      start()
                      isPlaying = true
                    }
                  }
                  setOnCompletionListener { isPlaying = false; position = duration; seeking = false }
                  setOnSeekCompleteListener { seeking = false; position = it.currentPosition }
                  setOnErrorListener { _, _, _ ->
                    isPreparing = false; isPlaying = false; prepared = false; error = true
                    true
                  }
                }
                player!!.setDataSource(context, Uri.parse(url), mapOf("User-Agent" to "Yanga Android", "Referer" to "https://bbs.nga.cn/"))
                player!!.prepareAsync()
              }.onFailure { isPreparing = false; prepared = false; error = true }
            }
          },
        ) {
          if (isPreparing) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
          else Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null)
        }
        Column(Modifier.weight(1f)) {
          Text("音频", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
      }
      MediaSeekBar(position, duration, prepared && !error, "音频进度") { target ->
        seeking = true
        position = target
        player?.seekTo(target)
      }
      if (error) Text("暂时无法播放，可重试或打开原链接", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
      TextButton(onClick = { onOpenLink(url) }, modifier = Modifier.align(Alignment.End)) { Text("打开原链接") }
    }
  }
}
