package com.yanga.client.ui.components

import android.net.Uri
import android.media.MediaPlayer
import android.os.Build
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.yanga.client.ui.content.PostContentPart
import kotlinx.coroutines.delay

@Composable
fun PostVideoPlayer(part: PostContentPart.Video, onOpenLink: (String) -> Unit, modifier: Modifier = Modifier) {
  var opened by remember(part.url) { mutableStateOf(false) }
  val preview by produceState<MediaPreview?>(null, part.url, part.direct) {
    value = MediaPreviewLoader.load(part.url, part.direct)
  }
  Surface(modifier.fillMaxWidth().semantics { contentDescription = "Video ${part.label}" },
    shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow) {
    Column {
      Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(MaterialTheme.colorScheme.surfaceContainerHighest)
        .clickable { if (part.direct) opened = true else onOpenLink(part.url) }, contentAlignment = Alignment.Center) {
        preview?.frame?.let { Image(it.asImageBitmap(), "视频封面", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        preview?.cover?.let { AsyncImage(it, "媒体封面", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        FilledTonalButton(onClick = { if (part.direct) opened = true else onOpenLink(part.url) },
          contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)) {
          Icon(Icons.Filled.PlayArrow, null)
          Spacer(Modifier.width(8.dp))
          Text(if (part.direct) "播放视频" else "打开媒体链接")
        }
      }
      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (part.direct) "视频" else "网页媒体", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(preview?.title ?: if (part.direct) "视频 · ${part.label}" else part.label,
          style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(Uri.parse(part.url).host ?: "本地视频", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (part.direct) TextButton(onClick = { onOpenLink(part.url) }, modifier = Modifier.align(Alignment.End)) { Text("打开原链接") }
      }
    }
  }
  if (opened) FullScreenVideo(part.url, preview?.title ?: part.label, { opened = false }, onOpenLink)
}

@Composable
private fun FullScreenVideo(url: String, title: String, onDismiss: () -> Unit, onOpenLink: (String) -> Unit) {
  var video by remember(url) { mutableStateOf<VideoView?>(null) }
  var player by remember(url) { mutableStateOf<MediaPlayer?>(null) }
  var prepared by remember(url) { mutableStateOf(false) }
  var playing by remember(url) { mutableStateOf(false) }
  var error by remember(url) { mutableStateOf(false) }
  var duration by remember(url) { mutableIntStateOf(0) }
  var position by remember(url) { mutableIntStateOf(0) }
  var seeking by remember(url) { mutableStateOf(false) }
  var controlsVisible by remember(url) { mutableStateOf(true) }
  var attempt by remember(url) { mutableIntStateOf(0) }
  var ratio by remember(url) { mutableFloatStateOf(16f / 9f) }
  val owner = LocalLifecycleOwner.current
  LaunchedEffect(prepared) {
    while (prepared) {
      if (!seeking) position = video?.currentPosition ?: 0
      delay(250)
    }
  }
  DisposableEffect(owner, url) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE) { video?.pause(); playing = false }
    }
    owner.lifecycle.addObserver(observer)
    onDispose { owner.lifecycle.removeObserver(observer); video?.stopPlayback(); video = null; player = null }
  }
  Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
    MediaViewerTheme {
      BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black).semantics { contentDescription = "全屏视频" }) {
        if (!error) key(attempt) {
          val videoModifier = if (maxWidth / maxHeight > ratio) Modifier.fillMaxHeight().aspectRatio(ratio) else Modifier.fillMaxWidth().aspectRatio(ratio)
          AndroidView(factory = { context ->
            VideoView(context).apply {
              video = this
              setOnPreparedListener { media ->
                player = media
                duration = media.duration.coerceAtLeast(0)
                if (media.videoWidth > 0 && media.videoHeight > 0) ratio = media.videoWidth.toFloat() / media.videoHeight
                prepared = true
                media.setOnSeekCompleteListener { seeking = false; position = it.currentPosition }
                if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) { start(); playing = true }
              }
              setOnCompletionListener { playing = false; position = duration; seeking = false; controlsVisible = true }
              setOnErrorListener { _, _, _ -> error = true; prepared = false; playing = false; controlsVisible = true; true }
              setVideoURI(Uri.parse(url), mapOf("User-Agent" to "Yanga Android", "Referer" to "https://bbs.nga.cn/"))
            }
          }, onRelease = { it.stopPlayback(); if (video === it) { video = null; player = null } }, modifier = videoModifier.align(Alignment.Center))
        }
        Box(Modifier.matchParentSize().clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClickLabel = if (controlsVisible) "隐藏视频控件" else "显示视频控件",
        ) { controlsVisible = !controlsVisible })
        if (!prepared && !error) CircularProgressIndicator(Modifier.align(Alignment.Center))
        if (error) Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
          Text("暂时无法播放", color = Color.White)
          TextButton(onClick = { error = false; seeking = false; attempt++ }) { Text("重试播放") }
          TextButton(onClick = { onOpenLink(url) }) { Text("打开原链接") }
        }
        if (controlsVisible) {
          MediaViewerHeader(title, "视频", "关闭视频", onDismiss, Modifier.align(Alignment.TopCenter))
          Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .96f)) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              FilledIconButton(modifier = Modifier.size(56.dp), enabled = prepared, onClick = {
                if (playing) video?.pause() else video?.start()
                playing = !playing
              }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "暂停视频" else "继续播放视频") }
              MediaSeekBar(position, duration, prepared, "视频进度") { target ->
                player?.let { media ->
                  seeking = true
                  position = target
                  // VideoView.seekTo uses the previous keyframe, which can jump back several seconds.
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) media.seekTo(target.toLong(), MediaPlayer.SEEK_CLOSEST)
                  else media.seekTo(target)
                }
              }
            }
          }
        }
      }
    }
  }
}
