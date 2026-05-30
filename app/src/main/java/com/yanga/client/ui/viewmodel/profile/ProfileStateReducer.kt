package com.yanga.client.ui

import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.ProfileReadData

internal fun MainContentUiState.withProfileResult(
  session: LoginSessionData,
  result: Result<ProfileReadData>,
): MainContentUiState =
  result.fold(
    onSuccess = { data ->
      copy(
        profile =
          profile.copy(
            session = LoadableUiState.Content(session.copy(avatarUrl = data.avatarUrl)),
            counters = LoadableUiState.Content(data.counters.toPreviews()),
            notifications = LoadableUiState.Content(data.notifications.map { it.toPreview() }),
          ),
      )
    },
    onFailure = { error ->
      copy(
        profile =
          profile.copy(
            counters = error.toLoadableError(),
            notifications = error.toLoadableError(),
          ),
      )
    },
  )
