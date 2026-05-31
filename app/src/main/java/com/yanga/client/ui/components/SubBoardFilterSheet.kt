package com.yanga.client.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.yanga.client.ui.SubBoardFilterLogic
import com.yanga.client.ui.SubBoardOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubBoardFilterSheet(
  boardName: String,
  options: List<SubBoardOption>,
  selectedIds: Set<String>,
  onDismiss: () -> Unit,
  onApply: (Set<String>) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
  var pendingSelection by remember(selectedIds, options) {
    mutableStateOf(if (selectedIds.isEmpty()) options.map { it.id }.toSet() else selectedIds)
  }
  val allPendingSelected = pendingSelection.size == options.size
  val blockedCount = (options.size - pendingSelection.size).coerceAtLeast(0)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "选择子版块",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = buildString {
            append(boardName)
            append(" · 已屏蔽 ")
            append(blockedCount)
            append('/')
            append(options.size)
          },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        TextButton(
          onClick = { pendingSelection = options.map { it.id }.toSet() },
        ) {
          Text("全选")
        }
        TextButton(
          onClick = {
            pendingSelection = if (selectedIds.isEmpty()) options.map { it.id }.toSet() else selectedIds
          },
        ) {
          Text("重置")
        }
      }

      HorizontalDivider()

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 360.dp),
      ) {
        items(options, key = { it.id }) { option ->
          val checked = option.id in pendingSelection
          ListItem(
            headlineContent = { Text(option.name) },
            trailingContent = {
              Checkbox(
                checked = checked,
                onCheckedChange = { isChecked ->
                  pendingSelection =
                    if (isChecked) {
                      pendingSelection + option.id
                    } else {
                      val next = pendingSelection - option.id
                      if (next.isEmpty()) options.map { it.id }.toSet() else next
                    }
                },
              )
            },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }

      Button(
        onClick = {
          val normalized =
            if (pendingSelection.size == options.size) {
              emptySet()
            } else {
              SubBoardFilterLogic.validateSelection(pendingSelection, options)
            }
          onApply(normalized)
        },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 16.dp),
      ) {
        Text("应用筛选")
      }
    }
  }
}
