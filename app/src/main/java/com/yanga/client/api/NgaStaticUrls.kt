package com.yanga.client.api

object NgaStaticUrls {
  fun boardIcon(fid: Int): String = "https://img4.nga.178.com/ngabbs/nga_classic/f/app/$fid.png"

  fun boardIconByStid(stid: Int): String = "https://img4.nga.178.com/proxy/cache_attach/ficon/${stid}v.png"

  fun expandRelativeImage(url: String): String =
    if (url.startsWith("./mon_")) {
      url.replace("./mon_", "http://img6.nga.178.com/attachments/mon_")
    } else {
      url
    }

  const val emoticonBaseUrl = "https://img4.nga.178.com/ngabbs/post/smile/"
}
