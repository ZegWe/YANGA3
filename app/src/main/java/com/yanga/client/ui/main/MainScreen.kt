package com.yanga.client.ui.main

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yanga.client.api.NgaPasswordLoginClient
import com.yanga.client.theme.YangaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

data class LoginSessionUiState(
  val username: String,
  val uid: String,
  val cookie: String,
)

@Composable
fun MainScreen(
  loginSession: LoginSessionUiState? = null,
  onLoginComplete: (LoginSessionUiState) -> Unit = {},
  onLogout: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var showLogin by remember { mutableStateOf(false) }
  var selectedTab by remember { mutableStateOf(MainTab.Home) }

  if (showLogin) {
    PasswordLoginScreen(
      onLoginComplete = {
        showLogin = false
        onLoginComplete(it)
      },
      onClose = { showLogin = false },
      modifier = modifier,
    )
    return
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    bottomBar = {
      YangaBottomNavigation(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
    },
  ) { paddingValues ->
    MainTabContent(
      selectedTab = selectedTab,
      loginSession = loginSession,
      onLoginClick = { showLogin = true },
      onLogout = onLogout,
      paddingValues = paddingValues,
    )
  }
}

@Composable
private fun MainTabContent(
  selectedTab: MainTab,
  loginSession: LoginSessionUiState?,
  onLoginClick: () -> Unit,
  onLogout: () -> Unit,
  paddingValues: PaddingValues,
) {
  val contentModifier =
    Modifier
      .fillMaxSize()
      .safeDrawingPadding()
      .padding(paddingValues)
      .padding(horizontal = 20.dp, vertical = 16.dp)

  when (selectedTab) {
    MainTab.Home -> HomeScreen(loginSession = loginSession, onLoginClick = onLoginClick, modifier = contentModifier)
    MainTab.Boards -> BoardsScreen(modifier = contentModifier)
    MainTab.Messages -> MessagesScreen(modifier = contentModifier)
    MainTab.Profile ->
      ProfileScreen(
        loginSession = loginSession,
        onLoginClick = onLoginClick,
        onLogout = onLogout,
        modifier = contentModifier,
      )
  }
}

@Composable
private fun YangaBottomNavigation(selectedTab: MainTab, onTabSelected: (MainTab) -> Unit) {
  NavigationBar {
    MainTab.entries.forEach { tab ->
      NavigationBarItem(
        selected = selectedTab == tab,
        onClick = { onTabSelected(tab) },
        icon = { Text(tab.label.take(1)) },
        label = { Text(tab.label) },
      )
    }
  }
}

@Composable
private fun PasswordLoginScreen(
  onLoginComplete: (LoginSessionUiState) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()
  val client = remember { NgaPasswordLoginClient() }
  var name by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var captcha by remember { mutableStateOf("") }
  var captchaId by remember { mutableStateOf(newCaptchaId()) }
  val pageId = remember { "P${Random.nextLong(100_000_000_000_000, 999_999_999_999_999)}" }
  var loading by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .safeDrawingPadding()
      .padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedButton(onClick = onClose) {
        Text(text = "关闭")
      }
      Text(
        text = "NGA 登录",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.weight(1f),
      )
      if (loading) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
      }
    }

    OutlinedTextField(
      value = name,
      onValueChange = {
        name = it
        error = null
      },
      enabled = !loading,
      label = { Text(text = "用户名 / 邮箱 / UID") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
      value = password,
      onValueChange = {
        password = it
        error = null
      },
      enabled = !loading,
      label = { Text(text = "密码") },
      visualTransformation = PasswordVisualTransformation(),
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CaptchaImage(
        captchaId = captchaId,
        modifier = Modifier.width(180.dp),
      )
      OutlinedButton(
        onClick = {
          captcha = ""
          captchaId = newCaptchaId()
        },
        enabled = !loading,
      ) {
        Text(text = "换一张")
      }
    }
    OutlinedTextField(
      value = captcha,
      onValueChange = {
        captcha = it
        error = null
      },
      enabled = !loading,
      label = { Text(text = "图形验证码") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
    error?.let {
      Text(
        text = it,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
      )
    }
    Button(
      enabled = !loading && name.isNotBlank() && password.isNotBlank() && captcha.isNotBlank(),
      onClick = {
        loading = true
        error = null
        scope.launch {
          val result = withContext(Dispatchers.IO) {
            runCatching { client.login(name, password, captchaId, captcha, pageId) }
          }
          loading = false
          result
            .onSuccess {
              onLoginComplete(
                LoginSessionUiState(
                  username = it.session.username,
                  uid = it.session.uid,
                  cookie = it.cookie,
                ),
              )
            }
            .onFailure {
              error = it.message ?: "登录失败"
              captcha = ""
              captchaId = newCaptchaId()
            }
        }
      },
    ) {
      Text(text = if (loading) "登录中" else "登录")
    }
  }
}

@Composable
private fun CaptchaImage(captchaId: String, modifier: Modifier = Modifier) {
  var image by remember(captchaId) { mutableStateOf<android.graphics.Bitmap?>(null) }
  LaunchedEffect(captchaId) {
    image = withContext(Dispatchers.IO) {
      runCatching {
        val connection = (URL("https://bbs.nga.cn/login_check_code.php?id=$captchaId&from=login").openConnection() as HttpURLConnection).apply {
          connectTimeout = 15_000
          readTimeout = 15_000
          setRequestProperty("Referer", "https://bbs.nga.cn/nuke.php?__lib=login&__act=login_ui")
          setRequestProperty("User-Agent", "Yanga Android")
        }
        connection.inputStream.use(BitmapFactory::decodeStream)
      }.getOrNull()
    }
  }

  if (image == null) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
      Text(text = "验证码加载中", style = MaterialTheme.typography.bodyMedium)
    }
  } else {
    Image(
      bitmap = image!!.asImageBitmap(),
      contentDescription = "图形验证码",
      modifier = modifier,
    )
  }
}

private fun newCaptchaId(): String =
  "login${Random.nextLong(100_000_000_000_000, 999_999_999_999_999)}"

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  YangaTheme { MainScreen() }
}

@Preview(showBackground = true, widthDp = 340)
@Composable
fun MainScreenPortraitPreview() {
  YangaTheme {
    MainScreen(
      loginSession = LoginSessionUiState(
        username = "测试用户",
        uid = "42",
        cookie = "ngaPassportUid=42; ngaPassportCid=abc",
      ),
    )
  }
}
