package com.yanga.client.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.yanga.client.ui.content.PostContentPart

@Composable
internal fun StructuredPostContent(
  part: PostContentPart,
  renderParts: @Composable (List<PostContentPart>) -> Unit,
) {
  ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
    when (part) {
      is PostContentPart.Album -> OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(part.title, style = MaterialTheme.typography.titleSmall)
          renderParts(part.images)
        }
      }
      is PostContentPart.Attachment -> {
        val context = androidx.compose.ui.platform.LocalContext.current
        val attachment = PostAttachmentPreview(name = part.name, url = part.url)
        var showDownload by remember(part) { mutableStateOf(false) }
        AttachmentRow(attachment, onClick = { showDownload = true })
        if (showDownload) AttachmentDownloadDialog(
          attachment = attachment,
          onDismiss = { showDownload = false },
          onConfirm = {
            showDownload = false
            com.yanga.client.ui.navigation.downloadAttachment(context, attachment)
          },
        )
      }
      is PostContentPart.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        part.items.forEachIndexed { index, item ->
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(listMarker(part.marker, index), Modifier.widthIn(min = 20.dp), style = MaterialTheme.typography.bodyMedium)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { renderParts(item) }
          }
        }
      }
      is PostContentPart.Heading -> Column(Modifier.fillMaxWidth().padding(top = 8.dp).semantics { heading() }) {
        ProvideTextStyle(MaterialTheme.typography.titleLarge) { renderParts(part.parts) }
      }
      is PostContentPart.Collapse -> {
        var expanded by rememberSaveable(part.title, part.parts) { mutableStateOf(false) }
        val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "Disclosure arrow")
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
          Column(Modifier.fillMaxWidth()) {
            Row(
              Modifier.fillMaxWidth().clickable(role = Role.Button) { expanded = !expanded }
                .semantics { stateDescription = if (expanded) "已展开" else "已折叠" }
                .heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text(part.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
              Icon(Icons.Default.ExpandMore, if (expanded) "收起" else "展开", Modifier.rotate(rotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(expanded) {
              Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) { renderParts(part.parts) }
            }
          }
        }
      }
      is PostContentPart.Code -> Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.small) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(part.language.ifBlank { "代码" }, style = MaterialTheme.typography.labelSmall)
          SelectionContainer {
            Text(part.text, Modifier.horizontalScroll(rememberScrollState()), fontFamily = FontFamily.Monospace,
              style = MaterialTheme.typography.bodySmall)
          }
        }
      }
      is PostContentPart.Table -> PostTable(part, renderParts)
      PostContentPart.Rule -> HorizontalDivider()
      else -> Unit
    }
  }
}

internal fun listMarker(type: String?, index: Int): String = when (type) {
  "1" -> "${index + 1}."
  "a", "A" -> {
    var value = index + 1
    var letters = ""
    while (value > 0) { value--; letters = ('a' + value % 26) + letters; value /= 26 }
    "${if (type == "A") letters.uppercase() else letters}."
  }
  "i", "I" -> {
    var value = index + 1
    val roman = buildString {
      for ((number, text) in listOf(1000 to "m", 900 to "cm", 500 to "d", 400 to "cd", 100 to "c", 90 to "xc", 50 to "l", 40 to "xl", 10 to "x", 9 to "ix", 5 to "v", 4 to "iv", 1 to "i")) {
        while (value >= number) { append(text); value -= number }
      }
    }
    "${if (type == "I") roman.uppercase() else roman}."
  }
  else -> "•"
}

private data class GridCell(val row: Int, val column: Int, val rowSpan: Int, val columnSpan: Int, val parts: List<PostContentPart>)

/** Place spans before measuring; cells are measured once, with no intrinsic queries on images. */
@Composable
private fun PostTable(table: PostContentPart.Table, renderParts: @Composable (List<PostContentPart>) -> Unit) {
  val cells = remember(table) {
    val occupied = mutableSetOf<Pair<Int, Int>>()
    buildList {
      table.rows.forEachIndexed { rowIndex, row ->
        var column = 0
        row.forEachIndexed { index, parts ->
          val span = table.spans.getOrNull(rowIndex)?.getOrNull(index) ?: PostContentPart.CellSpan()
          val rows = span.rows.coerceAtMost(table.rows.size - rowIndex)
          while ((0 until span.columns).any { (rowIndex to column + it) in occupied }) column++
          add(GridCell(rowIndex, column, rows, span.columns, parts))
          repeat(rows) { r -> repeat(span.columns) { c -> occupied += (rowIndex + r to column + c) } }
          column += span.columns
        }
      }
    }
  }
  if (cells.isEmpty()) return
  val columns = cells.maxOf { it.column + it.columnSpan }
  val border = MaterialTheme.colorScheme.outlineVariant
  val header = MaterialTheme.colorScheme.surfaceContainerHigh
  val rectangles = remember(table) { mutableListOf<Pair<Offset, Size>>() }
  BoxWithConstraints(Modifier.fillMaxWidth()) {
    val viewportWidth = maxWidth
    val columnWidth = (viewportWidth / columns).coerceAtLeast(112.dp)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      if (columnWidth * columns > viewportWidth) Text("左右滑动查看表格", style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
      Box(Modifier.horizontalScroll(rememberScrollState())) {
        Layout(
          content = { cells.forEach { cell ->
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { renderParts(cell.parts) }
          } },
          modifier = Modifier.width(columnWidth * columns).drawBehind {
            rectangles.forEachIndexed { index, (origin, size) ->
              if (cells[index].row == 0) drawRect(header, origin, size)
              drawRect(border, origin, size, style = Stroke(1.dp.toPx()))
            }
          },
        ) { measurables, constraints ->
          val unit = constraints.maxWidth / columns
          val placeables = measurables.mapIndexed { index, measurable ->
            measurable.measure(Constraints(maxWidth = unit * cells[index].columnSpan))
          }
          val heights = IntArray(table.rows.size) { 40.dp.roundToPx() }
          cells.forEachIndexed { index, cell ->
            if (cell.rowSpan == 1) heights[cell.row] = maxOf(heights[cell.row], placeables[index].height)
          }
          cells.forEachIndexed { index, cell ->
            val available = (cell.row until cell.row + cell.rowSpan).sumOf { heights[it] }
            val deficit = (placeables[index].height - available).coerceAtLeast(0)
            if (deficit > 0) heights[cell.row + cell.rowSpan - 1] += deficit
          }
          val tops = IntArray(heights.size + 1)
          heights.indices.forEach { tops[it + 1] = tops[it] + heights[it] }
          rectangles.clear()
          cells.forEach { cell -> rectangles += Offset((cell.column * unit).toFloat(), tops[cell.row].toFloat()) to
            Size((cell.columnSpan * unit).toFloat(), (tops[cell.row + cell.rowSpan] - tops[cell.row]).toFloat()) }
          layout(constraints.maxWidth, tops.last()) {
            placeables.forEachIndexed { index, placeable ->
              val cell = cells[index]
              placeable.placeRelative(cell.column * unit, tops[cell.row])
            }
          }
        }
      }
    }
  }
}
