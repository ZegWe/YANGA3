package com.yanga.client

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.yanga.client.data.DefaultNgaReadOnlyRepository
import com.yanga.client.data.SharedPreferencesFavoriteBoardsStore
import com.yanga.client.data.SharedPreferencesSubBoardFilterStore
import com.yanga.client.data.SubBoardFilterStore
import com.yanga.client.data.boards.BoardsCatalog
import com.yanga.client.data.boards.LocalBoardListStore
import com.yanga.client.data.image.ImageCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class YangaApplication : Application(), ImageLoaderFactory {
  lateinit var imageCacheManager: ImageCacheManager
    private set

  lateinit var repository: DefaultNgaReadOnlyRepository
    private set

  lateinit var boardsCatalog: BoardsCatalog
    private set

  lateinit var subBoardFilterStore: SubBoardFilterStore
    private set

  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override fun onCreate() {
    super.onCreate()
    imageCacheManager = ImageCacheManager(this) {
      if (::repository.isInitialized) repository.currentBaseUrl()
      else com.yanga.client.api.NgaDomains.BBS_NGA_CN
    }
    val preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    subBoardFilterStore = SharedPreferencesSubBoardFilterStore(preferences)
    val boardSectionDirectory = LocalBoardListStore(applicationContext, preferences)
    val favoriteBoardsStore = SharedPreferencesFavoriteBoardsStore(preferences)
    repository =
      DefaultNgaReadOnlyRepository(
        favoriteBoardsStore = favoriteBoardsStore,
        boardSectionDirectory = boardSectionDirectory,
      )
    boardsCatalog =
      BoardsCatalog(
        repository = repository,
        favoriteBoardsStore = favoriteBoardsStore,
        imageCacheManager = imageCacheManager,
        appContext = applicationContext,
        boardSectionDirectory = boardSectionDirectory,
        scope = applicationScope,
      )
    boardsCatalog.preloadAtStartup()
  }

  override fun newImageLoader(): ImageLoader = imageCacheManager.imageLoader

  private companion object {
    const val PREFS_NAME = "yanga_prefs"
  }
}
