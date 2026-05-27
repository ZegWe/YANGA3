package com.yanga.client.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yanga.client.theme.YangaTheme

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .safeDrawingPadding()
      .padding(24.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(text = "Yanga")
      Text(text = "API layer first. UI organization is pending.")
    }
  }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  YangaTheme { MainScreen() }
}

@Preview(showBackground = true, widthDp = 340)
@Composable
fun MainScreenPortraitPreview() {
  YangaTheme { MainScreen() }
}
