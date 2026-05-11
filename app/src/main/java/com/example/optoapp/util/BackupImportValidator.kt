package com.example.optoapp.util

import com.example.optoapp.data.BackupData
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * Validación defensiva antes de importar un respaldo JSON (tamaño, forma básica, límites de filas).
 */
object BackupImportValidator {

    private const val MAX_JSON_CHARS = 25_000_000
    private const val MAX_TOTAL_ROWS = 250_000

    fun parse(json: String): Result<BackupData> {
        if (json.isBlank()) {
            return Result.failure(IllegalArgumentException("Archivo vacío."))
        }
        if (json.length > MAX_JSON_CHARS) {
            return Result.failure(
                IllegalArgumentException("El archivo supera el tamaño máximo permitido para importación.")
            )
        }
        val parsed = try {
            Gson().fromJson(json, BackupData::class.java)
        } catch (e: JsonSyntaxException) {
            return Result.failure(IllegalArgumentException("JSON inválido o corrupto."))
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("No se pudo leer el respaldo: ${e.message}"))
        }
        val data = parsed ?: return Result.failure(IllegalArgumentException("Contenido no válido."))
        val total = (data.pacientes?.size ?: 0) +
            (data.evaluaciones?.size ?: 0) +
            (data.dispensaciones?.size ?: 0) +
            (data.pagos?.size ?: 0) +
            (data.serviciosExtra?.size ?: 0)
        if (total > MAX_TOTAL_ROWS) {
            return Result.failure(IllegalArgumentException("El respaldo contiene demasiados registros."))
        }
        return Result.success(data)
    }
}
