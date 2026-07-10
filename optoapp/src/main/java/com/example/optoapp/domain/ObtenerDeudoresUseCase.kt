package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.PacienteDao
import com.example.optoapp.data.Resource
import com.example.optoapp.data.pago.PagoDao
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

open class ObtenerDeudoresUseCase @Inject constructor(
    private val postgrest: Postgrest,
    private val repository: OptoRepository,
    private val pagoDao: PagoDao,
    private val pacienteDao: PacienteDao
) {
    companion object {
        private const val TAG = "ObtenerDeudores"
    }

    suspend operator fun invoke(opticaId: String): Resource<List<Deudor>> {
        return try {
            val jsonArray = callRpcDeudores(opticaId)
            val deudores = parseDeudoresFromJson(jsonArray)
            Resource.Success(deudores)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "Offline — trying local Room data for deudores", e)
            try {
                val deudores = fallbackToRoomDeudores(opticaId)
                Resource.Success(deudores, stale = true)
            } catch (ee: Exception) {
                Log.w(TAG, "Offline — no data available for deudores", ee)
                Resource.Error("No se pudieron cargar los datos de deudores")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo deudores", e)
            Resource.Error("No se pudieron cargar los datos de deudores")
        }
    }

    internal open suspend fun callRpcDeudores(opticaId: String): JsonArray =
        postgrest.rpc("rpc_deudores", buildJsonObject {
            put("p_optica_id", opticaId)
        }).decodeAs<JsonArray>()

    private fun parseDeudoresFromJson(arr: JsonArray): List<Deudor> {
        return arr.map { item ->
            val obj = item.jsonObject

            fun string(n: String) = obj[n]?.jsonPrimitive?.contentOrNull ?: ""
            fun double(n: String) = obj[n]?.jsonPrimitive?.doubleOrNull ?: 0.0
            fun int(n: String) = obj[n]?.jsonPrimitive?.intOrNull ?: 0

            Deudor(
                pacienteNombre = string("paciente_nombre"),
                pacienteTelefono = string("paciente_telefono"),
                ventaId = string("venta_id"),
                ventaFecha = try {
                    val raw = string("venta_fecha")
                    if (raw.isBlank()) java.time.LocalDate.MIN else LocalDate.parse(raw)
                } catch (e: java.time.format.DateTimeParseException) {
                    Log.w(TAG, "Invalid venta_fecha for deudor row, using LocalDate.MIN", e)
                    java.time.LocalDate.MIN
                },
                montoTotal = double("monto_total"),
                totalPagado = double("total_pagado"),
                saldo = double("saldo"),
                diasDeuda = int("dias_deuda"),
                pacienteId = string("paciente_id")
            )
        }
    }

    private suspend fun fallbackToRoomDeudores(opticaId: String): List<Deudor> {
        val pagos = pagoDao.getPagosListByOptica(opticaId)
        val pacientes = pacienteDao.getPacientesListByOptica(opticaId)

        val pagosPorDispensacion = pagos
            .filter { it.dispensacionId != null }
            .groupBy { it.dispensacionId!! }
            .mapValues { (_, pg) -> pg.sumOf { it.monto } }

        val pagosPorServicio = pagos
            .filter { it.servicioExtraId != null }
            .groupBy { it.servicioExtraId!! }
            .mapValues { (_, pg) -> pg.sumOf { it.monto } }

        val hoy = LocalDate.now()
        val deudores = mutableListOf<Deudor>()

        // Use repository to get dispensaciones and servicios directly — replaces Venta fallback
        val dispensaciones = repository.getAllDispensacionesForOptica(opticaId).first()
        for (disp in dispensaciones) {
            val totalPagado = pagosPorDispensacion[disp.id] ?: 0.0
            if (disp.montoTotal > totalPagado) {
                val paciente = pacientes.find { it.id == disp.pacienteId }
                deudores.add(
                    Deudor(
                        pacienteNombre = paciente?.nombreCompleto ?: "Paciente #${disp.pacienteId}",
                        pacienteTelefono = paciente?.telefono ?: "",
                        ventaId = disp.id,
                        ventaFecha = disp.fecha,
                        montoTotal = disp.montoTotal,
                        totalPagado = totalPagado,
                        saldo = disp.montoTotal - totalPagado,
                        diasDeuda = ChronoUnit.DAYS.between(disp.fecha, hoy).toInt(),
                        pacienteId = disp.pacienteId
                    )
                )
            }
        }

        val servicios = repository.getAllServiciosForOptica(opticaId).first()
        for (serv in servicios) {
            val totalPagado = pagosPorServicio[serv.id] ?: 0.0
            val montoTotal = serv.montoTotal
            if (montoTotal > totalPagado) {
                val paciente = serv.pacienteId?.let { pid -> pacientes.find { it.id == pid } }
                deudores.add(
                    Deudor(
                        pacienteNombre = paciente?.nombreCompleto ?: "Paciente #${serv.pacienteId ?: "?"}",
                        pacienteTelefono = paciente?.telefono ?: "",
                        ventaId = serv.id,
                        ventaFecha = serv.fecha,
                        montoTotal = montoTotal,
                        totalPagado = totalPagado,
                        saldo = montoTotal - totalPagado,
                        diasDeuda = ChronoUnit.DAYS.between(serv.fecha, hoy).toInt(),
                        pacienteId = serv.pacienteId ?: ""
                    )
                )
            }
        }

        return deudores.sortedByDescending { it.diasDeuda }
    }
}
