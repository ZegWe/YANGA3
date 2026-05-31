package com.yanga.client.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanga.client.ui.SubBoardFilterLogic
import com.yanga.client.ui.SubBoardOption

@Composable
internal fun SubBoardFilterBar(
  options: List<SubBoardOption>,
  selectedIds: Set<String>,
  onSelectAll: () -> Unit,
  onToggle: (String) -> Unit,
  onOpenSheet: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (options.size < 2) return

  val allSelected = selectedIds.isEmpty()
  val visibleOptions = SubBoardFilterLogic.visibleQuickOptions(options, selectedIds)
  val hiddenCount = (options.size - visibleOptions.size).coerceAtLeast(0)

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item(key = "all") {
        FilterChip(
          selected = allSelected,
          onClick = onSelectAll,
          label = { Text("全部") },
        )
      }
      items(visibleOptions, key = { it.id }) { option ->
        val enabled = SubBoardFilterLogic.isSubBoardEnabled(option.id, selectedIds)
        FilterChip(
          selected = enabled,
          onClick = { onToggle(option.id) },
          label = { Text(option.name) },
          leadingIcon =
            if (enabled) {
              {
                Icon(
                  imageVector = Icons.Filled.Check,
                  contentDescription = null,
                  modifier = Modifier.padding(start = 2.dp),
                )
              }
            } else {
              null
            },
        )
      }
      if (hiddenCount > 0) {
        item(key = "more") {
          AssistChip(
            onClick = onOpenSheet,
            label = { Text("+$hiddenCount") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = null,
              )
            },
          )
        }
      }
    }
  }
}
