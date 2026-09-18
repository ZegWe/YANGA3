package com.yanga.client.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.*

@Composable
internal fun LocalQrScannerScreen(onResult: (String) -> Unit, onBack: () -> Unit) {
  val context = LocalContext.current
  val owner = LocalLifecycleOwner.current
  var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
  var requested by rememberSaveable { mutableStateOf(false) }
  var cameraError by remember { mutableStateOf<String?>(null) }
  var preview by remember { mutableStateOf<BarcodeView?>(null) }
  var torch by rememberSaveable { mutableStateOf(false) }
  var retry by remember { mutableIntStateOf(0) }
  val onScanned by rememberUpdatedState(onResult)
  val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
  LaunchedEffect(Unit) {
    if (!granted && !requested) { requested = true; permission.launch(Manifest.permission.CAMERA) }
  }
  DisposableEffect(owner, preview) {
    val view = preview
    fun resume() {
      granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
      if (granted) view?.resume() else view?.pause()
    }
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> resume()
        Lifecycle.Event.ON_PAUSE -> view?.pause()
        else -> Unit
      }
    }
    owner.lifecycle.addObserver(observer)
    if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resume()
    onDispose { owner.lifecycle.removeObserver(observer); view?.pause() }
  }
  ProfilePageScaffold("扫一扫", onBack) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
      Text("对准 NGA 登录二维码", style = MaterialTheme.typography.headlineSmall)
      Text("识别后进入官方页面确认授权", style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
      val frameColor = MaterialTheme.colorScheme.primaryContainer
      Box(Modifier.widthIn(max = 420.dp).fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.extraLarge)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
        if (granted && cameraError == null) {
          key(retry) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
              BarcodeView(ctx).apply {
                setUseTextureView(true)
                decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
                marginFraction = 0.1
                addStateListener(object : CameraPreview.StateListener {
                  override fun previewSized() = Unit
                  override fun previewStarted() = Unit
                  override fun previewStopped() = Unit
                  override fun cameraClosed() = Unit
                  override fun cameraError(error: Exception) { cameraError = "无法打开相机，请检查相机是否被其他应用占用" }
                })
                decodeSingle(object : BarcodeCallback {
                  override fun barcodeResult(result: BarcodeResult) { pause(); onScanned(result.text) }
                  override fun possibleResultPoints(points: List<ResultPoint>) = Unit
                })
                preview = this
              }
            }, update = { it.setTorch(torch) }, onRelease = { it.pause(); if (preview === it) preview = null })
          }
          Canvas(Modifier.fillMaxSize()) {
            val inset = size.width * .1f
            drawRoundRect(frameColor, topLeft = Offset(inset, inset),
              size = androidx.compose.ui.geometry.Size(size.width - 2 * inset, size.height - 2 * inset),
              cornerRadius = CornerRadius(20.dp.toPx()), style = Stroke(3.dp.toPx()))
          }
        } else {
          Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Outlined.PhotoCamera, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text(cameraError ?: "允许相机权限后开始扫码", textAlign = TextAlign.Center)
            Button(onClick = {
              if (granted) { cameraError = null; retry++ }
              else permission.launch(Manifest.permission.CAMERA)
            }) { Text(if (granted) "重试" else "允许相机权限") }
            if (!granted) TextButton(onClick = {
              context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            }) { Text("打开设置") }
          }
        }
      }
      if (granted && cameraError == null && context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
        FilledTonalButton(onClick = { torch = !torch }, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)) {
          Icon(if (torch) Icons.Outlined.FlashlightOff else Icons.Outlined.FlashlightOn, null)
          Spacer(Modifier.width(8.dp))
          Text(if (torch) "关闭补光灯" else "打开补光灯")
        }
      }
      Text("仅支持 NGA 登录二维码。识别二维码不会自动授权，请在下一步确认。",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center)
    }
  }
}
