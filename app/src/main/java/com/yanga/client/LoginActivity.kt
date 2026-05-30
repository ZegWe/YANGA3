package com.yanga.client

import android.app.Activity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yanga.client.ui.PasswordLoginScreen

class LoginActivity : YangaComposeActivity() {
  @Composable
  override fun Content() {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    PasswordLoginScreen(
      onLoginComplete = { session ->
        onLoginComplete(session)
        setResult(Activity.RESULT_OK)
        finish()
      },
      onClose = { backDispatcher?.onBackPressed() },
      modifier = Modifier.fillMaxSize(),
    )
  }
}
