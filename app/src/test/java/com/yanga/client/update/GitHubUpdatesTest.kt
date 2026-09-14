package com.yanga.client.update

import org.junit.Assert.*
import org.junit.Test

class GitHubUpdatesTest {
    private fun release(code: String = "12", extra: String = "", url: String = "$PROJECT_URL/releases/download/v1.2/yanga-1.2-$code-release.apk", digest: String = "a".repeat(64)): String = """
        {"tag_name":"v1.2",$extra "assets":[{"name":"yanga-1.2-$code-release.apk","browser_download_url":"$url","size":123,"digest":"sha256:$digest"}]}
    """.trimIndent()

    @Test fun readsAndroidVersionCodeAndDigest() {
        val parsed = parseRelease(release())!!
        assertEquals(12L, parsed.code)
        assertEquals("1.2", parsed.version)
        assertEquals(123L, parsed.size)
        assertEquals("a".repeat(64), parsed.sha256)
    }
    @Test fun ignoresDraftsAndPrereleases() {
        assertNull(parseRelease(release(extra = "\"draft\":true,")))
        assertNull(parseRelease(release(extra = "\"prerelease\":true,")))
    }
    @Test fun rejectsUnexpectedDownloadHost() {
        assertThrows(IllegalArgumentException::class.java) { parseRelease(release(url = "https://example.com/update.apk")) }
    }
    @Test fun requiresDigest() {
        assertThrows(IllegalArgumentException::class.java) { parseRelease(release(digest = "")) }
    }
    @Test fun rejectsMissingApk() {
        assertThrows(IllegalArgumentException::class.java) { parseRelease("""{"tag_name":"v1.2","assets":[]}""") }
    }
    @Test fun rejectsOverflowCode() {
        assertThrows(IllegalArgumentException::class.java) { parseRelease(release(code = "2100000001")) }
    }
    @Test fun rejectsDuplicateApks() {
        val single = release()
        val asset = org.json.JSONObject(single).getJSONArray("assets").getJSONObject(0)
        val json = org.json.JSONObject(single)
        json.getJSONArray("assets").put(asset)
        assertThrows(IllegalArgumentException::class.java) { parseRelease(json.toString()) }
    }
}
