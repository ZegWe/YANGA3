package com.yanga.client.ui

import android.content.Context
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.linkify.LinkifyPlugin

internal fun releaseNotesRenderer(context: Context, onOpenUrl: (String) -> Unit): Markwon =
  Markwon.builder(context)
    .usePlugin(StrikethroughPlugin.create())
    .usePlugin(TablePlugin.create(context))
    .usePlugin(TaskListPlugin.create(context))
    .usePlugin(LinkifyPlugin.create())
    .usePlugin(object : AbstractMarkwonPlugin() {
      override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
        builder.linkResolver { _, link ->
          if (link.startsWith("https://") || link.startsWith("http://")) onOpenUrl(link)
        }
      }
    })
    .build()

@Composable
internal fun ReleaseNotesText(notes: String, onOpenUrl: (String) -> Unit) {
  val context = LocalContext.current
  val open by rememberUpdatedState(onOpenUrl)
  val renderer = remember(context) { releaseNotesRenderer(context) { open(it) } }
  val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
  val linkColor = MaterialTheme.colorScheme.primary.toArgb()
  val fontSize = MaterialTheme.typography.bodyMedium.fontSize.value
  AndroidView(
    modifier = Modifier.fillMaxWidth(),
    factory = { TextView(it).apply { movementMethod = LinkMovementMethod.getInstance() } },
    update = {
      it.setTextColor(textColor)
      it.setLinkTextColor(linkColor)
      it.textSize = fontSize
      renderer.setMarkdown(it, notes)
    },
  )
}
