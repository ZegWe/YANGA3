package com.yanga.client.ui

internal fun String?.initialOrFallback(): String =
  this?.trim()?.takeIf { it.isNotEmpty() }?.take(1) ?: "#"
