package com.yanga.client.data.boards

import com.yanga.client.api.NgaStaticUrls

/** Fallback when bundled JSON has no [NgaBoardSummary.iconUrl]. */
object BoardIconResolver {
  fun networkIconUrl(boardId: String): String? {
    val parts = boardId.split('_')
    val fid = parts.firstOrNull()?.toIntOrNull() ?: 0
    val stid = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return when {
      stid != 0 -> NgaStaticUrls.boardIconByStid(stid)
      fid != 0 -> NgaStaticUrls.boardIcon(fid)
      else -> null
    }
  }
}
