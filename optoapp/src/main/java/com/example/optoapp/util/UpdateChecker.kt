package com.example.optoapp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.optoapp.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checkea si hay una versión nueva usando una cadena de respaldo:
 *
 * 1. [tryCheckGithub] — API pública de GitHub Releases (sin auth, pública).
 * 2. [tryCheckSupabase] — consulta la tabla `app_releases` en Supabase.
 *
 * Si el primario falla (rate limit, sin red), cae al secundario.
 * Si ambos fallan, retorna null silenciosamente.
 *
 * El CI (build-apk.yml) crea GitHub Releases y llama a la Edge Function
 * track-release que inserta en `app_releases`.
 */
object UpdateChecker {

    private const val GITHUB_REPO = "MrSunstrider/Optoapp-2-0"
    private const val GITHUB_API = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    private const val GITHUB_WEB = "https://github.com/$GITHUB_REPO/releases/latest"

    @Serializable
    private data class GitHubRelease(
        @SerialName("tag_name") val tagName: String = "",
        @SerialName("html_url") val htmlUrl: String = "",
        val assets: List<GitHubAsset> = emptyList(),
    )

    @Serializable
    private data class GitHubAsset(
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
        val releaseUrl: String
    )

    // ─── Cadena de respaldo ─────────────────────────────────────────────

    /**
     * @param supabase Si se provee, se usa como respaldo si GitHub falla.
     */
    suspend fun check(supabase: SupabaseClient? = null): UpdateInfo? {
        // 1 — GitHub Releases API (primario)
        tryCheckGithub()?.let { return it }

        // 2 — Supabase app_releases (respaldo)
        if (supabase != null) {
            tryCheckSupabase(supabase)?.let { return it }
        }

        return null
    }

    // ─── Primario: GitHub API ───────────────────────────────────────────

    /**
     * Consulta la API pública de GitHub Releases.
     * No requiere autenticación (60 req/hora para IPs públicas).
     */
    private suspend fun tryCheckGithub(): UpdateInfo? {
        return try {
            val json = URL(GITHUB_API).readTextWithTimeout()
            val release: GitHubRelease = jsonParser.decodeFromString(json)
            val tag = release.tagName.removePrefix("v")
            val current = BuildConfig.VERSION_NAME

            if (!isNewer(tag, current)) return null

            val apkUrl = release.assets
                .firstOrNull { it.browserDownloadUrl.isNotBlank() }
                ?.browserDownloadUrl

            UpdateInfo(
                latestVersion = tag,
                downloadUrl = apkUrl ?: release.htmlUrl,
                releaseUrl = release.htmlUrl.ifBlank { GITHUB_WEB },
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Lee el body de una URL con timeout de 8s. */
    private fun URL.readTextWithTimeout(): String {
        val conn = openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout = 8_000
        return conn.inputStream.bufferedReader().readText()
    }

    // ─── Respaldo: Supabase ─────────────────────────────────────────────

    /**
     * Consulta la tabla `app_releases` en Supabase.
     * El CI inserta aquí via Edge Function track-release.
     */
    private suspend fun tryCheckSupabase(supabase: SupabaseClient): UpdateInfo? {
        return try {
            val releases = supabase.postgrest["app_releases"]
                .select {
                    order("version", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<AppRelease>()

            val latestRelease = releases.firstOrNull() ?: return null
            val current = BuildConfig.VERSION_NAME
            val latest = latestRelease.version

            if (!isNewer(latest, current)) return null

            UpdateInfo(
                latestVersion = latest,
                downloadUrl = latestRelease.apkDownloadUrl,
                releaseUrl = latestRelease.apkDownloadUrl,
            )
        } catch (_: Exception) {
            null
        }
    }

    // ─── Descarga e instalación ─────────────────────────────────────────

    /**
     * Descarga el APK y abre el intent de instalación.
     */
    fun downloadAndInstall(context: Context, url: String) {
        try {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apk = File(dir, "optoapp-update.apk")

            URL(url).openStream().use { input ->
                FileOutputStream(apk).use { output ->
                    input.copyTo(output)
                }
            }

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
            context.startActivity(intent)
        } catch (_: Exception) {
            // Si falla la descarga, abrir la URL en el navegador
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (_: Exception) { }
        }
    }

    /** Compara versiones semánticas (ej: "1.2.3" > "1.1.9"). */
    private fun isNewer(latest: String, current: String): Boolean {
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
}

/** Singleton Json con ignoreUnknownKeys para tolerar campos extra de la API de GitHub. */
private val jsonParser = Json { ignoreUnknownKeys = true }
