package com.example.optoapp.domain.command

import com.example.optoapp.util.AppLogger
import kotlinx.coroutines.CancellationException
import java.io.IOException

interface Command {
    suspend fun execute(): Result<String>
    suspend fun undo(): Result<Unit> = Result.success(Unit)
}

class BackupCommand(
    private val opticaId: String,
    private val repository: com.example.optoapp.data.OptoRepository,
) : Command {
    override suspend fun execute(): Result<String> = try {
        val data = repository.getBackupDataForOptica(opticaId)
        Result.success("Backup generado exitosamente")
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        AppLogger.e("BackupCommand", "Error en red generando backup: ${e.message}", e)
        Result.failure(e)
    } catch (e: Exception) {
        AppLogger.e("BackupCommand", "Error inesperado generando backup: ${e.message}", e)
        Result.failure(e)
    }
}

class ExportReportCommand(
    private val reportType: String,
    private val data: List<Any>,
) : Command {
    override suspend fun execute(): Result<String> {
        return Result.success("Reporte $reportType exportado")
    }
}

class CommandInvoker {
    private val history = mutableListOf<Command>()

    suspend fun executeCommand(command: Command): Result<String> {
        val result = command.execute()
        if (result.isSuccess) {
            history.add(command)
        }
        return result
    }

    suspend fun undoLast() {
        if (history.isNotEmpty()) {
            val command = history.removeAt(history.size - 1)
            command.undo()
        }
    }
}
