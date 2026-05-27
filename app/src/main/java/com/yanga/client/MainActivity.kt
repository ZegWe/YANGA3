package com.yanga.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.yanga.client.theme.YangaTheme
import com.yanga.client.ui.main.MainScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      YangaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
          MainScreen()
        }
      }
    }
  }
}
