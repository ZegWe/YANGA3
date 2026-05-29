package com.yanga.client.data

import com.yanga.client.api.NgaBoardSummary

data class LocalFavoriteBoard(
  val boardId: String,
  val name: String,
  val iconUrl: String? = null,
  val category: String = "",
)

interface FavoriteBoardsStore {
  fun list(): List<LocalFavoriteBoard>

  fun upsert(board: LocalFavoriteBoard)

  fun remove(boardId: String)
}

fun LocalFavoriteBoard.toBoardSummary(): NgaBoardSummary =
  NgaBoardSummary(
    boardId = boardId,
    name = name,
    iconUrl = iconUrl,
  )
