package com.yanga.client.ui

import androidx.compose.runtime.Composable
import com.yanga.client.data.NgaReadOnlyRepository

/** Replies use the same MD3 editor, formatting tools and attachment workflow as topics. */
@Composable
fun ReplyComposer(
  title: String,
  model: TopicComposerViewModel,
  repository: NgaReadOnlyRepository,
  loginSession: LoginSessionUiState?,
  onLogin: () -> Unit,
  onWeb: () -> Unit,
) = TopicComposer(title, 0, model, repository, loginSession, onLogin, onWeb)
