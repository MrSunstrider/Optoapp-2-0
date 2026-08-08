package com.example.optoapp.util

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun isNewer_returnsTrue_whenLatestIsHigher() {
        assertTrue(UpdateChecker.isNewer("1.2.3", "1.2.2"))
        assertTrue(UpdateChecker.isNewer("2.0.0", "1.9.9"))
        assertTrue(UpdateChecker.isNewer("1.0.1", "1.0.0"))
    }

    @Test
    fun isNewer_returnsFalse_whenLatestIsLower() {
        assertFalse(UpdateChecker.isNewer("1.2.2", "1.2.3"))
        assertFalse(UpdateChecker.isNewer("0.9.0", "1.0.0"))
        assertFalse(UpdateChecker.isNewer("1.0.0", "2.0.0"))
    }

    @Test
    fun isNewer_returnsFalse_whenVersionsAreEqual() {
        assertFalse(UpdateChecker.isNewer("1.2.3", "1.2.3"))
        assertFalse(UpdateChecker.isNewer("0.0.1", "0.0.1"))
    }

    @Test
    fun isNewer_handlesDifferentSegmentLengths() {
        assertTrue(UpdateChecker.isNewer("1.2.3.4", "1.2.3"))
        assertFalse(UpdateChecker.isNewer("1.2.3", "1.2.3.4"))
        assertTrue(UpdateChecker.isNewer("2.0", "1.9.9"))
    }

    @Test
    fun isNewer_handlesPreReleaseSuffixes() {
        // 'v' prefix removal happens before isNewer, so "1.0.0-beta" vs "1.0.0"
        // Según implementación actual, beta se parsea como 0 al fallback, así que no es mayor
        assertFalse(UpdateChecker.isNewer("1.0.0-beta", "1.0.0"))
    }

    @Test
    fun isNewer_handlesNonNumericSegmentsGracefully() {
        assertFalse(UpdateChecker.isNewer("latest", "1.0.0"))
        assertTrue(UpdateChecker.isNewer("2.0.0", "latest"))
    }

    @Test
    fun isNewer_handlesEmptyStrings() {
        assertFalse(UpdateChecker.isNewer("", "1.0.0"))
        assertTrue(UpdateChecker.isNewer("1.0.0", ""))
    }

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parseGitHubRelease_withMinimalFields() {
        val raw = """{
            "tag_name": "v1.5.0",
            "html_url": "https://github.com/repo/release",
            "assets": [
                { "browser_download_url": "https://github.com/repo/release/app-debug.apk" }
            ]
        }
        """.trimIndent()

        val release = json.decodeFromString<UpdateChecker.GitHubRelease>(raw)
        assertEquals("v1.5.0", release.tagName)
        assertEquals("https://github.com/repo/release", release.htmlUrl)
        assertEquals(1, release.assets.size)
        assertEquals("https://github.com/repo/release/app-debug.apk", release.assets[0].browserDownloadUrl)
    }

    @Test
    fun parseGitHubRelease_handlesEmptyAssets() {
        val raw = """{
            "tag_name": "v1.5.0",
            "html_url": "https://github.com/repo/release",
            "assets": []
        }
        """.trimIndent()

        val release = json.decodeFromString<UpdateChecker.GitHubRelease>(raw)
        assertTrue(release.assets.isEmpty())
    }

    @Test
    fun parseAppRelease_withMinimalFields() {
        val raw = """{
            "version": "1.5.0",
            "apk_download_url": "https://supabase.co/storage/apk/app-debug.apk"
        }
        """.trimIndent()

        val release = json.decodeFromString<UpdateChecker.AppRelease>(raw)
        assertEquals("1.5.0", release.version)
        assertEquals("https://supabase.co/storage/apk/app-debug.apk", release.apkDownloadUrl)
    }

    @Test
    fun downloadResult_success_isObject() {
        val result = UpdateChecker.DownloadResult.Success
        assertEquals(UpdateChecker.DownloadResult.Success, result)
    }

    @Test
    fun downloadResult_error_hasMessage() {
        val result = UpdateChecker.DownloadResult.Error("connection failed")
        assertEquals("connection failed", (result as UpdateChecker.DownloadResult.Error).message)
    }

    // --- findLatestByVersion tests ---

    private fun appRelease(version: String) = UpdateChecker.AppRelease(version, "https://example.com/$version.apk")

    @Test
    fun findLatestByVersion_returnsHighestSemanticVersion_notLexicographicFirst() {
        val releases = listOf(
            appRelease("1.9.0"),
            appRelease("1.15.8"),
            appRelease("1.10.0"),
        )
        val result = UpdateChecker.findLatestByVersion(releases)
        assertEquals("1.15.8", result?.version)
    }

    @Test
    fun findLatestByVersion_returnsHighestWhenSegmentsDiffer() {
        val releases = listOf(
            appRelease("2.0"),
            appRelease("1.9.9"),
            appRelease("1.10.1"),
        )
        val result = UpdateChecker.findLatestByVersion(releases)
        assertEquals("2.0", result?.version)
    }

    @Test
    fun findLatestByVersion_returnsOnlyVersionForSingleElementList() {
        val releases = listOf(appRelease("1.0.0"))
        val result = UpdateChecker.findLatestByVersion(releases)
        assertEquals("1.0.0", result?.version)
    }

    @Test
    fun findLatestByVersion_returnsNullForEmptyList() {
        val releases = emptyList<UpdateChecker.AppRelease>()
        val result = UpdateChecker.findLatestByVersion(releases)
        assertEquals(null, result)
    }

    @Test
    fun findLatestByVersion_handlesIdenticalVersions() {
        val releases = listOf(
            appRelease("1.5.0"),
            appRelease("1.5.0"),
        )
        val result = UpdateChecker.findLatestByVersion(releases)
        assertTrue(result != null)
        assertEquals("1.5.0", result?.version)
    }

    @Test
    fun findLatestByVersion_handlesMajorJump() {
        val releases = listOf(
            appRelease("1.15.8"),
            appRelease("2.0.0"),
            appRelease("1.20.3"),
        )
        val result = UpdateChecker.findLatestByVersion(releases)
        assertEquals("2.0.0", result?.version)
    }
}
