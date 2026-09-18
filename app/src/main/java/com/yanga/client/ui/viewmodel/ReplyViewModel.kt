package com.yanga.client.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ReplyViewModel(private val saved: SavedStateHandle) : ViewModel() {
  val visible = saved.getStateFlow("visible", false)
  val content = saved.getStateFlow("content", "")
  val pid = saved.getStateFlow("pid", "0")
  val target = saved.getStateFlow("target", "回复主题")
  val sending = MutableStateFlow(false)
  val error = MutableStateFlow<String?>(null)
  val sent = MutableStateFlow(false)

  fun open(post: PostPreview?) {
    // Resume an existing draft with its original recipient.
    if (content.value.isBlank()) {
      saved["pid"] = post?.pid?.ifBlank { "0" } ?: "0"
      saved["target"] = post?.let { "回复 #${it.floorNumber} ${it.author}" } ?: "回复主题"
    }
    saved["visible"] = true
  }
  fun edit(value: String) { saved["content"] = value }
  fun close() { if (!sending.value) saved["visible"] = false }
  fun submit(repository: NgaReadOnlyRepository, session: LoginSessionData?, tid: String) {
    if (sending.value || content.value.isBlank()) return
    sending.value = true
    error.value = null
    viewModelScope.launch {
      try {
        repository.submitReply(session, tid, pid.value, content.value).fold(
          onSuccess = {
            saved["content"] = ""
            saved["visible"] = false
            sent.value = true
          },
          onFailure = { error.value = it.message ?: "发送失败，请查看主题后重试" },
        )
      } finally { sending.value = false }
    }
  }
}
