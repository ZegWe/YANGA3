package com.yanga.client.update

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class UpdateState(val message: String = "检查 GitHub 上的最新正式版本", val busy: Boolean = false,
    val progress: Int? = null, val release: AppRelease? = null, val apk: File? = null)

class AboutViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val updates = GitHubUpdates()
    private val downloadDirectory = File(app.cacheDir, "updates/${java.util.UUID.randomUUID()}")
    private val mutableState = MutableStateFlow(UpdateState())
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    private val installed = app.packageManager.getPackageInfo(app.packageName, 0)
    val versionLabel = "${installed.versionName} (${PackageInfoCompat.getLongVersionCode(installed)})"
    val isDebug = app.packageName.endsWith(".debug")

    fun check() {
        if (mutableState.value.busy) return
        job = viewModelScope.launch {
            mutableState.value = UpdateState(message = "正在检查更新…", busy = true)
            try {
                val release = updates.latest()
                mutableState.value = when {
                    release == null -> UpdateState(message = "尚无正式版本发布")
                    isDebug -> UpdateState(message = "当前为调试版，正式版使用独立包名。请从项目页面安装正式版。")
                    release.code <= PackageInfoCompat.getLongVersionCode(installed) -> UpdateState(message = "当前已是最新版本")
                    else -> UpdateState(message = "发现新版本 ${release.version}", release = release)
                }
            } catch (e: CancellationException) {
                mutableState.value = UpdateState("已取消，可重新检查或下载", release = mutableState.value.release)
                throw e
            }
            catch (e: Exception) { mutableState.value = UpdateState(message = "检查失败：${e.localizedMessage ?: "网络不可用"}") }
        }
    }

    fun download() {
        val release = mutableState.value.release ?: return
        if (mutableState.value.busy) return
        job = viewModelScope.launch {
            mutableState.value = UpdateState("正在下载…", true, 0, release)
            try {
                val apk = updates.download(release, downloadDirectory) { percent ->
                    mutableState.value = mutableState.value.copy(progress = percent)
                }
                validate(apk, release)
                mutableState.value = UpdateState("下载完成，点击安装更新", release = release, apk = apk)
            } catch (e: CancellationException) {
                mutableState.value = UpdateState("已取消，可重新检查或下载", release = mutableState.value.release)
                throw e
            }
            catch (e: Exception) { mutableState.value = UpdateState("下载失败：${e.localizedMessage ?: "网络不可用"}", release = release) }
        }
    }

    fun cancel() {
        mutableState.value = mutableState.value.copy(message = "正在取消…", busy = true, progress = null)
        job?.cancel()
    }

    @Suppress("DEPRECATION")
    private fun validate(file: File, release: AppRelease) {
        val candidate = app.packageManager.getPackageArchiveInfo(file.path, PackageManager.GET_SIGNATURES)
            ?: error("APK 无法解析，请重新下载")
        val current = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_SIGNATURES)
        require(candidate.packageName == app.packageName && PackageInfoCompat.getLongVersionCode(candidate) == release.code
            && candidate.versionName == release.version) { "APK 包名或版本与发布信息不符" }
        require(!candidate.signatures.isNullOrEmpty() && candidate.signatures!!.toSet() == current.signatures?.toSet()) {
            "更新签名与当前应用不一致，无法覆盖安装"
        }
    }

    fun install() {
        val value = mutableState.value
        val apk = value.apk ?: return
        try {
            require(apk.isFile) { "下载文件已被清理，请重新下载" }
            validate(apk, value.release ?: error("缺少版本信息"))
            if (Build.VERSION.SDK_INT >= 26 && !app.packageManager.canRequestPackageInstalls()) {
                app.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                mutableState.value = value.copy(message = "请允许此来源安装应用，返回后再次点击安装更新")
                return
            }
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.updates", apk)
            app.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION))
            mutableState.value = value.copy(message = "已打开系统安装界面；若取消，可再次点击安装更新")
        } catch (e: Exception) { mutableState.value = value.copy(message = "无法安装：${e.localizedMessage}") }
    }
}
