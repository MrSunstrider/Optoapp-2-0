package com.example.optoapp.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.optoapp.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks for new versions using a fallback chain:
 *
 * 1. [tryCheckGitHub] — GitHub Releases public API (no auth).
 * 2. [tryCheckSupabase] — queries `app_releases` in Supabase.
 *
 * If the primary fails (rate limit, no network), it falls back to the secondary.
 * If both fail, returns null silently.
 *
 * CI (build-apk.yml) creates GitHub Releases and calls the track-release
 * Edge Function which inserts into `app_releases`.
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val GITHUB_REPO = "MrSunstrider/Optoapp-2-0"
    private const val GITHUB_API = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    private const val GITHUB_WEB = "https://github.com/$GITHUB_REPO/releases/latest"

    @Serializable
    internal data class GitHubRelease(
        @SerialName("tag_name") val tagName: String = "",
        @SerialName("html_url") val htmlUrl: String = "",
        val assets: List<GitHubAsset> = emptyList(),
    )

    @Serializable
    internal data class GitHubAsset(
        @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    )

    @Serializable
    data class AppRelease(
        val version: String = "",
        @SerialName("apk_download_url")
        val apkDownloadUrl: String = "",
    )

    data class UpdateInfo(
        val latestVersion: String,
        val downloadUrl: String,
        val releaseUrl: String,
    )

    /** Result of the download/install operation. */
    sealed class DownloadResult {
        data object Success : DownloadResult()
        data class Error(val message: String) : DownloadResult()
        /** The APK was downloaded but the app lacks package install permission. */
        data object NeedsInstallPermission : DownloadResult()
    }

    /**
     * @param supabase Si se provee, se usa como respaldo si GitHub falla.
     */
    suspend fun check(supabase: SupabaseClient? = null): UpdateInfo? {
        // 1 — GitHub Releases API (primario)
        tryCheckGitHub()?.let { return it }

        // 2 — Supabase app_releases (respaldo)
        if (supabase != null) {
            tryCheckSupabase(supabase)?.let { return it }
        }

        return null
    }

    /**
     * GitHub's public API with 60 req/hour for public IPs — no auth required.
     *
     * ONLY returns [UpdateInfo] if the release has a real APK asset.
     * If the release exists but has no APK asset, returns null so the
     * fallback chain (Supabase) can take over.
     */
    private suspend fun tryCheckGitHub(): UpdateInfo? {
        return try {
            val json = withContext(Dispatchers.IO) {
                URL(GITHUB_API).readTextWithTimeout()
            }
            val release: GitHubRelease = jsonParser.decodeFromString(json)
            val tag = release.tagName.removePrefix("v")
            val current = BuildConfig.VERSION_NAME

            if (!isNewer(tag, current)) return null

            val apkUrl = release.assets
                .firstOrNull { it.browserDownloadUrl.isNotBlank() && it.browserDownloadUrl.endsWith(".apk") }
                ?.browserDownloadUrl

            // release.htmlUrl is HTML, not APK — let Supabase handle it.
            if (apkUrl == null) return null

            UpdateInfo(
                latestVersion = tag,
                downloadUrl = apkUrl,
                releaseUrl = release.htmlUrl.ifBlank { GITHUB_WEB },
            )
        } catch (e: Exception) {
            AppLogger.w(TAG, "GitHub API falló, se intentará Supabase como respaldo: ${e.message}", e)
            null
        }
    }

    private fun URL.readTextWithTimeout(): String {
        val conn = openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            return conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }

    /**
     * CI inserts here via the track-release Edge Function.
     */
    private suspend fun tryCheckSupabase(supabase: SupabaseClient): UpdateInfo? {
        return try {
            val releases = supabase.postgrest["app_releases"]
                .select()
                .decodeList<AppRelease>()

            val latestRelease = findLatestByVersion(releases) ?: return null
            val current = BuildConfig.VERSION_NAME
            val latest = latestRelease.version

            if (!isNewer(latest, current)) return null

            UpdateInfo(
                latestVersion = latest,
                downloadUrl = latestRelease.apkDownloadUrl,
                releaseUrl = latestRelease.apkDownloadUrl,
            )
        } catch (e: Exception) {
            AppLogger.w(TAG, "Supabase app_releases falló: ${e.message}", e)
            null
        }
    }

    /**
     * The UI decides how to handle each error case — never open the browser from here.
     * Use [openDownloadInBrowser] as a manual fallback from the UI.
     *
     * @return [DownloadResult.NeedsInstallPermission] if the APK was downloaded
     *   but REQUEST_INSTALL_PACKAGES is not granted.
     */
    suspend fun downloadAndInstall(
        context: Context,
        url: String,
    ): DownloadResult {
        // Use applicationContext to avoid leaks if the Activity dies during download
        val appContext = context.applicationContext
        return withContext(Dispatchers.IO) {
            try {
                if (!isOnline(appContext)) {
                    return@withContext DownloadResult.Error("No hay conexión a Internet")
                }

                val dir = File(appContext.cacheDir, "updates").apply { mkdirs() }
                val apk = File(dir, "optoapp-update.apk")

                if (apk.exists()) apk.delete()

                val conn = URL(url).openConnection() as HttpURLConnection
                var expectedSize = -1L
                try {
                    conn.connectTimeout = 15_000
                    conn.readTimeout = 300_000 // 5 min — APK is ~15 MB, mobile networks need more time
                    conn.setInstanceFollowRedirects(true)
                    conn.connect()

                    expectedSize = conn.contentLengthLong

                    conn.inputStream.use { input ->
                        FileOutputStream(apk).use { output ->
                            input.copyTo(output)
                        }
                    }
                } finally {
                    conn.disconnect()
                }

                val validationError = verifyApk(apk, expectedSize)
                if (validationError != null) {
                    apk.delete()
                    AppLogger.w(TAG, "APK validation failed: $validationError")
                    return@withContext DownloadResult.Error("Descarga corrupta: $validationError")
                }
                // Android < 8 does not require this permission
                val canInstall = withContext(Dispatchers.Main) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        appContext.packageManager.canRequestPackageInstalls()
                    } else {
                        true
                    }
                }

                if (!canInstall) {
                    AppLogger.w(TAG, "Install permission not granted, prompting user")
                    return@withContext DownloadResult.NeedsInstallPermission
                }

                withContext(Dispatchers.Main) {
                    launchInstallIntent(appContext, apk, url)
                }

                DownloadResult.Success
            } catch (e: CancellationException) {
                // Clean up partial download and propagate cancellation
                val apk = File(appContext.cacheDir, "updates/optoapp-update.apk")
                if (apk.exists()) apk.delete()
                throw e
            } catch (e: Exception) {
                // Clean up partial download on failure
                val apk = File(appContext.cacheDir, "updates/optoapp-update.apk")
                if (apk.exists()) apk.delete()
                val msg = e.localizedMessage ?: "Error al descargar la actualización"
                AppLogger.e(TAG, "Download failed: $msg", e)
                DownloadResult.Error(msg)
            }
        }
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Opens the download URL in the browser as a manual fallback.
     * Only use when [downloadAndInstall] failed and the user wants to retry manually.
     * Only https URLs are allowed for security.
     */
    fun openDownloadInBrowser(context: Context, url: String) {
        val uri = Uri.parse(url)
        if (uri.scheme != "https") {
            AppLogger.w(TAG, "openDownloadInBrowser: URL scheme no es https: $url")
            Toast.makeText(context, "URL de descarga insegura", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "No hay navegador disponible. Abrí manualmente:\n$url", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "openDownloadInBrowser failed", e)
            Toast.makeText(context, "Error al abrir navegador. Link:\n$url", Toast.LENGTH_LONG).show()
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "openInstallPermissionSettings failed", e)
            Toast.makeText(context, "No se pudo abrir Ajustes. Buscá 'Instalar apps desconocidas'.", Toast.LENGTH_LONG).show()
        }
    }

    private fun verifyApk(file: File, expectedSize: Long): String? {
        if (!file.exists()) return "archivo no encontrado"
        if (file.length() == 0L) return "archivo vacío"
        if (file.length() < 1024) return "archivo demasiado pequeño (${file.length()} bytes)"

        if (expectedSize > 0L && file.length() != expectedSize) {
            return "tamaño incorrecto: esperado $expectedSize, descargado ${file.length()}"
        }

        // Los APK son archivos ZIP — deben empezar con "PK"
        val magicBytes = file.inputStream().use { input ->
            val header = ByteArray(2)
            if (input.read(header) != 2) return "archivo demasiado pequeño para ser APK"
            header
        }
        if (magicBytes[0] != 0x50.toByte() || magicBytes[1] != 0x4B.toByte()) {
            return "el archivo descargado no es un APK válido (formato incorrecto)"
        }

        return null
    }

    private fun launchInstallIntent(context: Context, apk: File, fallbackUrl: String) {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apk,
            )
        } else {
            Uri.fromFile(apk)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            openDownloadInBrowser(context, fallbackUrl)
        }
    }

    fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }

    /**
     * PostgREST ORDER BY version DESC uses lexicographic ordering, so "1.9.0"
     * ranks above "1.15.8". We compare segments numerically to avoid this.
     */
    fun findLatestByVersion(releases: List<AppRelease>): AppRelease? {
        return releases.maxWithOrNull(Comparator { a, b ->
            val segsA = a.version.split(".").map { it.toIntOrNull() ?: 0 }
            val segsB = b.version.split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(segsA.size, segsB.size)) {
                val av = segsA.getOrElse(i) { 0 }
                val bv = segsB.getOrElse(i) { 0 }
                if (av != bv) return@Comparator av - bv
            }
            0
        })
    }
}

/** Singleton Json with ignoreUnknownKeys to tolerate extra fields from the GitHub API. */
private val jsonParser = Json { ignoreUnknownKeys = true }
