package com.example.optoapp.domain.command

interface Command {
    suspend fun execute(): Result<String>
    suspend fun undo(): Result<Unit> = Result.success(Unit) // Opcional
}

class BackupCommand(
    private val opticaId: String,
    private val repository: com.example.optoapp.data.OptoRepository
) : Command {
    override suspend fun execute(): Result<String> {
        return try {
            val data = repository.getBackupDataForOptica(opticaId)
            // Lógica para guardar en archivo o nube
            Result.success("Backup generado exitosamente")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ExportReportCommand(
    private val reportType: String,
    private val data: List<Any>
) : Command {
    override suspend fun execute(): Result<String> {
        // Lógica de exportación a PDF/Excel
        return Result.success("Reporte $reportType exportado")
    }
}

/**
 * Invocador que gestiona la cola de comandos y el historial para deshacer.
 */
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
