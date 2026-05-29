package com.yanga.client.ui

sealed interface LoadableUiState<out T> {
  object Loading : LoadableUiState<Nothing>
  data class Error(val message: String, val cause: Throwable? = null) : LoadableUiState<Nothing>
  data class Empty(val message: String) : LoadableUiState<Nothing>
  object LoginRequired : LoadableUiState<Nothing>
  data class Content<T>(val value: T) : LoadableUiState<T>
}
