package com.yanga.client.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanga.client.update.AboutViewModel
import com.yanga.client.update.PROJECT_URL

@Composable
internal fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val model = viewModel<AboutViewModel> { AboutViewModel(application) }
    val state by model.state.collectAsStateWithLifecycle()
    var linkError by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回") }
            Text("关于", style = MaterialTheme.typography.headlineMedium)
        }
        Text("Yanga", style = MaterialTheme.typography.headlineLarge)
        Text("NGA 论坛 Android 客户端 · Kotlin / Jetpack Compose")
        Text("当前版本：${model.versionLabel}${if (model.isDebug) " · 调试版" else ""}")
        TextButton(onClick = {
            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_URL))) }
            catch (_: Exception) { linkError = "无法打开浏览器，请访问 $PROJECT_URL" }
        }) { Text("GitHub 项目主页") }
        linkError?.let { Text(it) }
        HorizontalDivider()
        Text(state.message)
        state.progress?.let { percent ->
            LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
            Text("$percent%")
        }
        if (state.busy) {
            if (state.progress == null) CircularProgressIndicator()
            TextButton(onClick = model::cancel) { Text("取消") }
        } else {
            Button(onClick = model::check) { Text("检查更新") }
            state.release?.let {
                Text("版本 ${it.version} · ${it.size / 1024 / 1024} MB")
                if (state.apk != null) Button(onClick = model::install) { Text("安装更新") }
                OutlinedButton(onClick = model::download) { Text(if (state.apk == null) "下载更新 APK" else "重新下载") }
                if (it.notes.isNotBlank()) Text(it.notes)
            }
        }
    }
}
