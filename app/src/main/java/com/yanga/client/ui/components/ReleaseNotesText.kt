package com.yanga.client.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.style.TextDecoration

@Composable
internal fun ReleaseNotesText(notes: String, onOpenUrl: (String) -> Unit) {
  val links = Regex("""\[([^\]]+)\]\((https?://[^\s)]+)\)|(https?://[^\s<>]+)""")
  val style = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
  val text = buildAnnotatedString {
    var offset = 0
    for (match in links.findAll(notes)) {
      append(notes.substring(offset, match.range.first))
      val markdown = match.groupValues[2].isNotEmpty()
      val raw = if (markdown) match.groupValues[2] else match.value
      val url = if (markdown) raw else raw.trimEnd('.', ',', ';', ':', ')', ']', '，', '。', '；')
      withLink(LinkAnnotation.Url(url, style) { onOpenUrl(url) }) {
        append(if (markdown) match.groupValues[1] else url)
      }
      if (!markdown) append(raw.substring(url.length))
      offset = match.range.last + 1
    }
    append(notes.substring(offset))
  }
  Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
