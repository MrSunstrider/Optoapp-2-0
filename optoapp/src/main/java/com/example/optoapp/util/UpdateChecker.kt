package com.example.optoapp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.optoapp.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
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

    /** Resultado de la operación de descarga/instalación. */
    sealed class DownloadResult {
        data object Success : DownloadResult()
        data class Error(val message: String) : DownloadResult()
    }

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

    /**
     * Consulta la API pública de GitHub Releases.
     * No requiere autenticación (60 req/hora para IPs públicas).
     *
     * SOLO retorna [UpdateInfo] si el release tiene un asset APK real.
     * Si el release existe pero sin asset APK ([) -> retorna null para que
     * la cadena de respaldo (Supabase) pueda tomar el control.
     */
    private suspend fun tryCheckGithub(): UpdateInfo? {
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

            // NO caer a release.htmlUrl — eso es HTML, no APK.
            // Si no hay asset APK, que lo resuelva Supabase.
            if (apkUrl == null) return null

            UpdateInfo(
                latestVersion = tag,
                downloadUrl = apkUrl,
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

    /**
     * Descarga el APK en segundo plano, lo valida y abre el intent de instalación.
     *
     * @param context Contexto de la aplicación.
     * @param url URL de descarga del APK.
     * @param onError Callback opcional para notificar errores al usuario.
     * @return [DownloadResult] indicando si la operación fue exitosa.
     */
    suspend fun downloadAndInstall(
        context: Context,
        url: String,
        onError: ((String) -> Unit)? = { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        },
    ): DownloadResult {
        return withContext(Dispatchers.IO) {
            try {
                val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                val apk = File(dir, "optoapp-update.apk")

                // Limpiar descarga previa fallida
                if (apk.exists()) apk.delete()

                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 30_000
                conn.connect()

                // Obtener tamaño esperado antes de descargar
                val expectedSize = conn.contentLengthLong

                conn.inputStream.use { input ->
                    FileOutputStream(apk).use { output ->
                        input.copyTo(output)
                    }
                }

                // Validar el APK descargado
                val validationError = verifyApk(apk, expectedSize)
                if (validationError != null) {
                    apk.delete()
                    val msg = "La descarga está corrupta: $validationError"
                    withContext(Dispatchers.Main) {
                        openInBrowser(context, url, msg, onError)
                    }
                    return@withContext DownloadResult.Error(msg)
                }

                withContext(Dispatchers.Main) {
                    launchInstallIntent(context, apk, url)
                }

                DownloadResult.Success
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Error al descargar la actualización"
                withContext(Dispatchers.Main) {
                    openInBrowser(context, url, msg, onError)
                }
                DownloadResult.Error(msg)
            }
        }
    }

    /**
     * Verifica que el archivo descargado sea un APK válido.
     *
     * Checks:
     * - Existe y no está vacío
     * - Tiene los bytes mágicos de un ZIP (PK) — todo APK es un ZIP
     * - Si sabíamos el tamaño esperado, verifica que coincida
     *
     * @return null si es válido, o un mensaje de error si no.
     */
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

    /** Lanza el intent de instalación del APK descargado. */
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
            openInBrowser(context, fallbackUrl, "No se pudo iniciar la instalación", null)
        }
    }

    /**
     * Abre la URL de descarga en el navegador como fallback.
     * En Android 11+ requiere `<queries>` en el manifest (ya agregado).
     */
    private fun openInBrowser(
        context: Context,
        url: String,
        logMessage: String,
        onError: ((String) -> Unit)?,
    ) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            onError?.invoke("$logMessage. Abrí el link manualmente: $url")
        }
    }

    /** Compara versiones semánticas (ej: "1.2.3" > "1.1.9"). */
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
}

/** Singleton Json con ignoreUnknownKeys para tolerar campos extra de la API de GitHub. */
private val jsonParser = Json { ignoreUnknownKeys = true }
