package com.yanga.client.api

import java.net.URLDecoder
import java.net.URLEncoder

object NgaEncoding {
  fun urlEncodeUtf8(value: String): String = URLEncoder.encode(value, "UTF-8")

  fun urlEncodeGbk(value: String): String = URLEncoder.encode(value, "GBK")

  fun urlDecodeGbk(value: String): String = URLDecoder.decode(value, "GBK")
}
