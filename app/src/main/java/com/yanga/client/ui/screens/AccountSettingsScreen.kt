package com.yanga.client.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import com.yanga.client.ui.components.UserAvatar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import kotlinx.coroutines.launch

@Composable
internal fun AccountSettingsScreen(repository: NgaReadOnlyRepository, session: LoginSessionData?, onBack: () -> Unit, onLogin: () -> Unit, onForumSettings: () -> Unit) {
  // Discard editor and requests when the account changes.
  key(session) { AccountSettingsEditor(repository, session, onBack, onLogin, onForumSettings) }
}

@Composable
private fun AccountSettingsEditor(repository: NgaReadOnlyRepository, session: LoginSessionData?, onBack: () -> Unit, onLogin: () -> Unit, onForumSettings: () -> Unit) {
  var signature by remember { mutableStateOf("") }
  var original by remember { mutableStateOf("") }
  var loaded by remember { mutableStateOf(false) }
  var loading by remember { mutableStateOf(true) }
  var saving by remember { mutableStateOf(false) }
  var editingSignature by remember { mutableStateOf(false) }
  var editingAvatar by remember { mutableStateOf(false) }
  var message by remember { mutableStateOf<String?>(null) }
  var avatarUrl by remember { mutableStateOf<String?>(null) }
  var uploadedAvatarUrl by remember { mutableStateOf<String?>(null) }
  var avatarSaving by remember { mutableStateOf(false) }
  var avatarMessage by remember { mutableStateOf<String?>(null) }
  var selectedImage by remember { mutableStateOf<ByteArray?>(null) }
  var preparingImage by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val snackbar = remember { SnackbarHostState() }
  var retry by remember { mutableIntStateOf(0) }
  val scope = rememberCoroutineScope()
  val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri != null) {
      preparingImage = true
      avatarMessage = null
      scope.launch {
        try {
          selectedImage = prepareAvatarImage(context.contentResolver, uri)
          uploadedAvatarUrl = null
        } catch (error: CancellationException) {
          throw error
        } catch (error: Exception) {
          avatarMessage = error.message ?: "读取图片失败，请重新选择"
        } finally { preparingImage = false }
      }
    }
  }
  LaunchedEffect(retry) {
    loading = true
    if (session != null) repository.loadUser(session, session.uid).fold(onSuccess = {
      avatarUrl = it.avatarUrl
      signature = it.signature; original = it.signature; loaded = true; message = null
    }, onFailure = { message = it.message ?: "读取资料失败" })
    loading = false
  }
  ProfilePageScaffold(title = "账号设置", onBack = onBack, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
    Column(
      Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      if (session == null) {
        ProfileSectionCard {
          Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary)
          Text("登录后管理账号", style = MaterialTheme.typography.titleLarge)
          Text("编辑个性签名，管理个人资料与账号安全。", color = MaterialTheme.colorScheme.onSurfaceVariant)
          Button(onClick = onLogin) { Text("登录 NGA") }
        }
      } else {
        ProfileSectionCard {
          Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            UserAvatar(session.username, avatarUrl, size = 56.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(session.username, style = MaterialTheme.typography.titleLarge)
              Text("UID ${session.uid}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!editingAvatar) TextButton(onClick = { editingAvatar = true; avatarMessage = null }, enabled = !loading) {
              Text("修改头像")
            }
          }
          AnimatedVisibility(editingAvatar) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().animateContentSize()) {
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              Text("选择新头像", style = MaterialTheme.typography.titleMedium)
              val preview = remember(selectedImage) { selectedImage?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (preview != null) {
                  Image(preview, "待上传头像预览", Modifier.size(80.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                  UserAvatar(session.username, avatarUrl, size = 72.dp)
                }
                FilledTonalButton(enabled = !avatarSaving && !preparingImage && !loading, onClick = { imagePicker.launch("image/*") }) {
                  Text(if (preparingImage) "正在处理图片…" else if (selectedImage == null) "选择图片" else "重新选择")
                }
              }
              avatarMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(enabled = !avatarSaving && !preparingImage, onClick = {
                  editingAvatar = false; selectedImage = null; uploadedAvatarUrl = null; avatarMessage = null
                }) { Text("取消") }
                Button(enabled = !loading && !avatarSaving && !preparingImage && selectedImage != null, onClick = {
                  val imageToUpload = selectedImage
                  avatarSaving = true
                  avatarMessage = null
                  scope.launch {
                    try {
                      val submittedUrl = uploadedAvatarUrl ?: if (imageToUpload != null) {
                        repository.uploadAvatar(session, imageToUpload).getOrThrow().also {
                          // Keep the uploaded URL if saving fails, so retry does not upload twice.
                          uploadedAvatarUrl = it
                        }
                      } else error("请先选择图片")
                      repository.saveAvatar(session, submittedUrl).fold(
                        onSuccess = {
                          avatarUrl = repository.loadUser(session, session.uid).getOrNull()?.avatarUrl ?: avatarUrl
                          selectedImage = null
                          uploadedAvatarUrl = null
                          editingAvatar = false
                          avatarSaving = false
                          snackbar.showSnackbar("头像已保存")
                        },
                        onFailure = { avatarMessage = it.message ?: "头像保存失败，请重试" },
                      )
                    } catch (error: CancellationException) {
                      throw error
                    } catch (error: Exception) {
                      avatarMessage = error.message ?: "头像上传失败，请重试"
                    } finally {
                      avatarSaving = false
                    }
                  }
                }) {
                  if (avatarSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                  }
                  Text(if (avatarSaving) "上传并保存中…" else "保存头像")
                }
              }
            }
          }
        }
        ProfileSectionCard {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.primary)
            Text("个性签名", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (!editingSignature && loaded) TextButton(onClick = { signature = original; message = null; editingSignature = true }) { Text("修改签名") }
          }
          if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
          if (loaded) {
            if (!editingSignature) {
              if (original.isBlank()) Text("暂未设置签名", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              else SignatureContent(original, Modifier.fillMaxWidth())
            }
            AnimatedVisibility(editingSignature) {
              Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().animateContentSize()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OutlinedTextField(
                  value = signature, onValueChange = { signature = it; message = null },
                  label = { Text("签名内容") }, supportingText = { Text("支持 NGA 标记，也可以留空清除签名") },
                  enabled = !saving, minLines = 4, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
                )
                if (signature.isNotBlank()) {
                  Text("签名预览", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                  Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                    SignatureContent(signature, Modifier.padding(16.dp))
                  }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                  TextButton(enabled = !saving, onClick = { signature = original; message = null; editingSignature = false }) { Text("取消") }
                  Button(enabled = !saving && signature != original, onClick = {
                    saving = true; message = null
                    scope.launch {
                      val result = repository.saveSignature(session, signature)
                      result.fold(onSuccess = { original = signature; editingSignature = false }, onFailure = { message = it.message ?: "保存失败，请重试" })
                      saving = false
                      if (result.isSuccess) snackbar.showSnackbar("签名已保存")
                    }
                  }) {
                    if (saving) {
                      CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                      Spacer(Modifier.width(8.dp))
                    }
                    Text(if (saving) "保存中…" else "保存签名")
                  }
                }
              }
            }
          } else if (!loading) {
            FilledTonalButton(onClick = { retry++ }) { Text("重新加载") }
          }
          message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        Card(
          onClick = onForumSettings, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
          ListItem(
            headlineContent = { Text("论坛个人中心与账号安全") },
            supportingContent = { Text("前往论坛个人中心管理") },
            leadingContent = { Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, "打开论坛个人中心") },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
          )
        }
      }
    }
  }
}
