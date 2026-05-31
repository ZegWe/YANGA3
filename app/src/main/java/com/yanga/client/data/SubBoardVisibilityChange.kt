package com.yanga.client.data

import com.yanga.client.api.NgaSubBoard

data class SubBoardVisibilityChange(
  val board: NgaSubBoard,
  val visible: Boolean,
)
