package com.yanga.client.ui

import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.LoginRequiredException

internal fun LoginSessionUiState.toData(): LoginSessionData =
  LoginSessionData(username = username, uid = uid, cookie = cookie)

internal fun Throwable.toLoadableError(): LoadableUiState<Nothing> =
  if (this is LoginRequiredException) LoadableUiState.LoginRequired
  else LoadableUiState.Error(message = message ?: "Unknown error", cause = this)
