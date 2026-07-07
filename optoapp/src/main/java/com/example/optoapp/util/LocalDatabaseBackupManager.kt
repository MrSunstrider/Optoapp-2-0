package com.example.optoapp.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * Respaldo local liviano de la base SQLite para recuperación ante incidentes de migración.
 */
object LocalDatabaseBackupManager {
    private const val TAG = "LocalDBBackup"
    private const val BACKUP_DIR = "db_backups"
    private const val MAX_BACKUPS = 7
    private const val MIN_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6 horas
    private const val PREFS_NAME = "opto_db_backup_prefs"
    private const val KEY_LAST_BACKUP_AT = "last_backup_at"

    fun backupIfNeeded(context: Context, dbName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val last = prefs.getLong(KEY_LAST_BACKUP_AT, 0L)
        if (now - last < MIN_INTERVAL_MS) return

        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return

        val backupDir = File(context.filesDir, BACKUP_DIR).apply { mkdirs() }
        val stamp = now.toString()
        copySafe(dbFile, File(backupDir, "$dbName-$stamp.db"))
        copySafe(File(dbFile.absolutePath + "-wal"), File(backupDir, "$dbName-$stamp.db-wal"))
        copySafe(File(dbFile.absolutePath + "-shm"), File(backupDir, "$dbName-$stamp.db-shm"))

        cleanupOldBackups(backupDir, dbName)
        prefs.edit().putLong(KEY_LAST_BACKUP_AT, now).apply()
    }

    private fun copySafe(source: File, target: File) {
        if (!source.exists()) return
        try {
            source.copyTo(target, overwrite = true)
        } catch (e: IOException) {
            Log.w(TAG, "Database backup failed", e)
        }
    }

    private fun cleanupOldBackups(dir: File, dbName: String) {
        val backups = dir.listFiles()
            ?.filter { it.name.startsWith("$dbName-") }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        backups.drop(MAX_BACKUPS).forEach { it.delete() }
    }
}
