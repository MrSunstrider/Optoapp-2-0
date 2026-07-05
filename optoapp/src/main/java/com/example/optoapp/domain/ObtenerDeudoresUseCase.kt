package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.Resource
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.venta.VentaDao
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
import kotlinx.serialization.json.put
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

open class ObtenerDeudoresUseCase @Inject constructor(
    private val postgrest: Postgrest,
    private val ventaDao: VentaDao,
    private val pagoDao: PagoDao
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
            Log.w(TAG, "Offline — no data available for deudores", e)
            Resource.Error("Sin conexion para obtener deudores: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo deudores", e)
            Resource.Error("Error obteniendo deudores: ${e.localizedMessage}")
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
                ventaFecha = LocalDate.parse(string("venta_fecha")),
                montoTotal = double("monto_total"),
                totalPagado = double("total_pagado"),
                saldo = double("saldo"),
                diasDeuda = int("dias_deuda")
            )
        }
    }
}
