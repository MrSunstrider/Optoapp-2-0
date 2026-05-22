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
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Checkea si hay una versión nueva consultando la tabla app_releases en Supabase.
 * El CI (build-apk.yml) inserta en esa tabla via Edge Function después de crear una GitHub Release.
 * Si hay versión nueva descarga el APK y abre el instalador.
 */
object UpdateChecker {

    @Serializable
    data class AppRelease(
        val version: String = "",
        @kotlinx.serialization.SerialName("apk_download_url")
        val apkDownloadUrl: String = "",
    )

    data class UpdateInfo(
        val latestVersion: String,
        val downloadUrl: String,
        val releaseUrl: String
    )

    /**
     * Consulta Supabase y retorna [UpdateInfo] si hay versión más nueva.
     * @param supabase Cliente Supabase ya autenticado (de la app).
     */
    suspend fun check(supabase: SupabaseClient): UpdateInfo? {
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
                releaseUrl = latestRelease.apkDownloadUrl
            )
        } catch (_: Exception) {
            null // Silencio si no hay red o la consulta falla
        }
    }

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
                    apk
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
