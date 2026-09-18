package com.yanga.client.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.yanga.client.ui.navigation.WebViewRoute
import com.yanga.client.web.NgaQrLoginLink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScanLoginScreen(session: LoginSessionUiState?, onLogin: () -> Unit, onBack: () -> Unit) {
  var scannedUrl by rememberSaveable { mutableStateOf<String?>(null) }
  var error by rememberSaveable { mutableStateOf<String?>(null) }
  val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
    result.contents?.let { raw ->
      val link = NgaQrLoginLink.parse(raw)
      if (link == null) error = "这不是支持的 NGA 登录二维码，请扫描另一台设备上的 NGA 登录二维码"
      else { scannedUrl = link.url; error = null }
    }
  }
  val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    if (!granted) error = "未获得相机权限，请允许相机权限后重试，或在系统设置中开启"
    else runCatching {
      scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        .setPrompt("扫描另一台设备上的 NGA 登录二维码")
        .setBeepEnabled(false).setOrientationLocked(false))
    }.onFailure { error = "无法打开相机，请检查相机是否可用后重试" }
  }
  val link = scannedUrl?.let(NgaQrLoginLink::parse)
  BackHandler(enabled = link != null) { scannedUrl = null }
  if (link != null && session != null) {
    WebViewRoute(
      pageUrl = link.url, fallbackTitle = "扫码授权 · ${session.username}",
      cookieBaseUrl = link.baseUrl, cookieHeader = session.cookie,
      onBack = { scannedUrl = null }, authorizationOnly = true,
    )
    return
  }
  ProfilePageScaffold(title = "扫一扫", onBack = onBack) { padding ->
    Column(
      Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(104.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(52.dp))
        }
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("授权其他设备登录", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
          "扫描 NGA 登录二维码，连接你的账号与另一台设备。",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
      ProfileSectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PersonOutline, null) }
          }
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (session == null) "请先登录你的 NGA 账号" else "当前账号", style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(session?.username ?: "登录后即可扫码授权", style = MaterialTheme.typography.titleMedium)
            session?.let { Text("UID ${it.uid}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
          }
        }
        Button(
          onClick = {
            if (session == null) onLogin()
            else { error = null; cameraPermission.launch(android.Manifest.permission.CAMERA) }
          },
          modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
          contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        ) {
          Icon(if (session == null) Icons.Outlined.PersonOutline else Icons.Outlined.QrCodeScanner, null, Modifier.size(20.dp))
          Spacer(Modifier.width(8.dp))
          Text(if (session == null) "登录 NGA" else "打开相机扫码")
        }
      }
      error?.let {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.onErrorContainer) {
          Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.ErrorOutline, null)
            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
          }
        }
      }
      Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("如何使用", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ScanInstruction(Icons.Outlined.Devices, "在另一台设备打开 NGA", "进入登录页面，显示登录二维码。")
        ScanInstruction(Icons.Outlined.QrCodeScanner, "对准二维码", "打开相机，将二维码放入取景框。")
        ScanInstruction(Icons.Outlined.VerifiedUser, "确认授权", "在官方页面确认后，另一台设备即可登录。")
      }
    }
  }
}

@Composable
private fun ScanInstruction(icon: ImageVector, title: String, description: String) {
  Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall)
      Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
