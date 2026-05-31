package com.yanga.client.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yanga.client.ui.SubBoardFilterLogic
import com.yanga.client.ui.SubBoardOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubBoardDirectorySheet(
  boardName: String,
  options: List<SubBoardOption>,
  selectedIds: Set<String>,
  onDismiss: () -> Unit,
  onSetSubBoardEnabled: (String, Boolean) -> Unit,
  onSelectAllSubBoards: () -> Unit,
  onOpenSubBoard: (SubBoardOption) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
  val selectedCount = if (selectedIds.isEmpty()) options.size else selectedIds.size

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "子版块",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = "$boardName · 显示 $selectedCount/${options.size}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      TextButton(
        onClick = onSelectAllSubBoards,
        modifier = Modifier.padding(horizontal = 12.dp),
      ) {
        Text("全部显示")
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

      LazyColumn(
        modifier =
          Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
      ) {
        items(options, key = { it.id }) { option ->
          val enabled = SubBoardFilterLogic.isSubBoardEnabled(option.id, selectedIds)
          ListItem(
            headlineContent = { Text(option.name) },
            trailingContent = {
              Switch(
                checked = enabled,
                onCheckedChange = { checked -> onSetSubBoardEnabled(option.id, checked) },
              )
            },
            modifier =
              Modifier
                .fillMaxWidth()
                .clickable { onOpenSubBoard(option) },
            overlineContent = { Text("点击进入子版块") },
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        }
      }
    }
  }
}
