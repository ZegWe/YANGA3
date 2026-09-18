package com.yanga.client.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class ScanLoginScreenTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test fun profileOffersScanEntry() {
    var opened = false
    compose.setContent { ProfileScreen(null, onLoginClick = {}, onLogout = {}, onScanClick = { opened = true }) }
    compose.onNodeWithText("扫一扫").performScrollTo().performClick()
    compose.runOnIdle { assertTrue(opened) }
  }

  @Test fun signedOutUsersMustLoginBeforeScanning() {
    var login = false
    compose.setContent { ScanLoginScreen(null, { login = true }, {}) }
    compose.onNodeWithText("打开相机扫码").assertDoesNotExist()
    compose.onNodeWithText("登录 NGA").performClick()
    compose.runOnIdle { assertTrue(login) }
  }

  @Test fun signedInUsersSeeAccountAndCameraAction() {
    compose.setContent { ScanLoginScreen(LoginSessionUiState("测试用户", "42", "cookie"), {}, {}) }
    compose.onNodeWithText("测试用户").assertExists()
    compose.onNodeWithText("UID 42").assertExists()
    compose.onNodeWithText("打开相机扫码").assertExists()
  }

  @Test fun fallbackDisplaysMaterialScannerAndBackAction() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.uiAutomation.executeShellCommand(
      "pm grant ${instrumentation.targetContext.packageName} android.permission.CAMERA",
    ).use { descriptor -> java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() } }
    compose.setContent { LocalQrScannerScreen({}, {}) }
    compose.onNodeWithText("对准 NGA 登录二维码").assertExists()
    compose.onNodeWithText("识别后进入官方页面确认授权").assertExists()
    compose.onNodeWithText("扫一扫").assertExists()
  }

  @Test fun cameraScannerCanBeOpenedAndCancelled() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.uiAutomation.executeShellCommand(
      "pm grant ${instrumentation.targetContext.packageName} android.permission.CAMERA",
    ).use { descriptor -> java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() } }
    val monitor = instrumentation.addMonitor("com.yanga.client.QrScannerActivity", null, false)
    try {
      compose.setContent { ScanLoginScreen(LoginSessionUiState("测试用户", "42", "cookie"), {}, {}) }
      compose.onNodeWithText("打开相机扫码").performClick()
      val scanner = instrumentation.waitForMonitorWithTimeout(monitor, 5_000)
      assertNotNull("Scanner activity should launch", scanner)
      instrumentation.runOnMainSync { scanner.finish() }
      compose.onNodeWithText("打开相机扫码").assertExists()
    } finally {
      instrumentation.removeMonitor(monitor)
    }
  }
}
