package com.yanga.client.data.boards

import com.yanga.client.api.NgaBoardSection

interface BoardSectionDirectory {
  fun loadSections(): List<NgaBoardSection>

  fun saveSections(sections: List<NgaBoardSection>)

  fun clear()

  fun lastIncrementalRequestAt(): Long

  fun markIncrementalRequested(at: Long)
}
