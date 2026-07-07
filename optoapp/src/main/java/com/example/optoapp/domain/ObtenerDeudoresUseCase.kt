package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.Resource
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.venta.VentaDao
import com.example.optoapp.data.PacienteDao
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CancellationException
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
    private val ventaDao: VentaDao,
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
                Resource.Success(deudores)
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
        val ventas = ventaDao.getAllVentasByOptica(opticaId)
        val pagos = pagoDao.getPagosListByOptica(opticaId)
        val pacientes = pacienteDao.getPacientesListByOptica(opticaId)

        val pagosPorVenta = pagos
            .filter { it.ventaId != null }
            .groupBy { it.ventaId!! }
            .mapValues { (_, pg) -> pg.sumOf { it.monto } }

        val hoy = LocalDate.now()

        return ventas
            .filter { v ->
                val totalPagado = pagosPorVenta[v.id] ?: 0.0
                v.montoTotal > totalPagado
            }
            .map { v ->
                val totalPagado = pagosPorVenta[v.id] ?: 0.0
                val paciente = pacientes.find { it.id == v.pacienteId }
                Deudor(
                    pacienteNombre = paciente?.nombreCompleto ?: "Paciente #${v.pacienteId}",
                    pacienteTelefono = paciente?.telefono ?: "",
                    ventaId = v.id,
                    ventaFecha = v.fecha,
                    montoTotal = v.montoTotal,
                    totalPagado = totalPagado,
                    saldo = v.montoTotal - totalPagado,
                    diasDeuda = ChronoUnit.DAYS.between(v.fecha, hoy).toInt(),
                    pacienteId = v.pacienteId
                )
            }
    }
}
