package com.yanga.client.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanga.client.R
import com.yanga.client.update.AboutViewModel
import com.yanga.client.update.PROJECT_URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val model = viewModel<AboutViewModel> { AboutViewModel(application) }
    val state by model.state.collectAsStateWithLifecycle()
    var linkError by remember { mutableStateOf<String?>(null) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                    Image(painterResource(R.mipmap.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(88.dp))
                }
                Text("Yanga", style = MaterialTheme.typography.headlineLarge)
                Text("让社区阅读更自在", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Text("当前版本：${model.versionLabel}${if (model.isDebug) " · 调试版" else ""}",
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                }
            }
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("应用更新", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(state.message, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.progress?.let { percent ->
                        LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
                        Text("已下载 $percent%", style = MaterialTheme.typography.labelMedium)
                    }
                    if (state.busy) {
                        if (state.progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                        TextButton(onClick = model::cancel) { Text("取消") }
                    } else if (state.release == null) {
                        Button(onClick = model::check, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("检查更新")
                        }
                    } else {
                        state.release?.let { release ->
                            Text("版本 ${release.version} · ${release.size / 1024 / 1024} MB", style = MaterialTheme.typography.labelLarge)
                            Button(onClick = if (state.apk != null) model::install else model::download,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text(if (state.apk != null) "安装更新" else "下载更新 APK")
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = model::check) { Text("检查更新") }
                                if (state.apk != null) TextButton(onClick = model::download) { Text("重新下载") }
                            }
                        }
                    }
                }
            }
            state.release?.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("更新内容", style = MaterialTheme.typography.titleMedium)
                    Text(notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("关于项目", Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                OutlinedCard(
                    onClick = {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_URL))) }
                        catch (_: Exception) { linkError = "无法打开浏览器，请访问 $PROJECT_URL" }
                    },
                    shape = MaterialTheme.shapes.large,
                ) {
                    ListItem(
                        headlineContent = { Text("GitHub 项目主页") },
                        supportingContent = { Text("源代码与版本动态") },
                        trailingContent = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, "在浏览器打开") },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    )
                }
                linkError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            Text("为 NGA 社区打造的 Android 客户端", Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
