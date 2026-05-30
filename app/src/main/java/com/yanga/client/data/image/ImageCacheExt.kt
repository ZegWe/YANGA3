package com.yanga.client.data.image

import android.content.Context
import com.yanga.client.YangaApplication

fun Context.imageCacheManager(): ImageCacheManager =
  (applicationContext as YangaApplication).imageCacheManager
