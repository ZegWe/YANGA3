package com.yanga.client.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.DragEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Composable
internal fun ComposerDropTarget(enabled: Boolean, onFiles: (List<android.net.Uri>) -> Unit) {
  val view = LocalView.current
  val context = LocalContext.current
  val callback = rememberUpdatedState(onFiles)
  val active = rememberUpdatedState(enabled)
  DisposableEffect(view) {
    val permissions = mutableListOf<android.view.DragAndDropPermissions>()
    view.setOnDragListener { _, event ->
      when (event.action) {
        DragEvent.ACTION_DRAG_STARTED -> active.value && event.clipDescription?.let {
          it.hasMimeType("image/*") || it.hasMimeType("application/*") || it.hasMimeType("video/*") || it.hasMimeType("audio/*") || it.hasMimeType("text/uri-list")
        } == true
        DragEvent.ACTION_DROP -> {
          if (!active.value) false else {
            var current: Context = context
            while (current is ContextWrapper && current !is Activity) current = current.baseContext
            (current as? Activity)?.requestDragAndDropPermissions(event)?.let(permissions::add)
            val clip = event.clipData
            val files = (0 until (clip?.itemCount ?: 0)).mapNotNull { clip?.getItemAt(it)?.uri }
            if (files.isNotEmpty()) callback.value(files)
            files.isNotEmpty()
          }
        }
        else -> true
      }
    }
    onDispose { view.setOnDragListener(null); permissions.forEach { it.release() } }
  }
}
