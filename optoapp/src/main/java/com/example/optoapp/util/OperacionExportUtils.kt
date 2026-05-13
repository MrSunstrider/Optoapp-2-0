package com.example.optoapp.util

import android.content.Context
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.Montura
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import java.io.File
import java.time.LocalDate

object OperacionExportUtils {
    fun exportPendientesCsv(
        context: Context,
        dispensacionesPendientes: List<DispensacionOptica>,
        serviciosPendientes: List<ServicioExtra>
    ): File {
        val rows = buildString {
            appendLine("tipo,id,fecha,paciente_id,estado,total,pagado/saldo")
            dispensacionesPendientes.forEach {
                appendLine("dispensacion,${it.id},${it.fecha},${it.pacienteId},${it.estadoEntrega},${it.montoTotal},${it.montoPagado}")
            }
            serviciosPendientes.forEach {
                appendLine("servicio_extra,${it.id},${it.fecha},${it.pacienteId.orEmpty()},${it.estado},${it.montoTotal},${it.aCuenta}")
            }
        }
        return writeCsv(context, "pendientes-operacion", rows)
    }

    fun exportCierreDiaCsv(context: Context, fecha: LocalDate, pagos: List<Pago>): File {
        val rows = buildString {
            appendLine("fecha_reporte,$fecha")
            appendLine("id,fecha,tipo,metodo_pago,monto,nota")
            pagos.forEach {
                appendLine("${it.id},${it.fecha},${it.tipo},${it.metodoPago},${it.monto},${it.nota.replace(",", " ")}")
            }
        }
        return writeCsv(context, "cierre-caja", rows)
    }

    fun exportInventarioCsv(context: Context, monturas: List<Montura>): File {
        val rows = buildString {
            appendLine("id,sku,marca,modelo,color,talla,stock_actual,stock_minimo,precio,costo,activo")
            monturas.forEach {
                appendLine("${it.id},${it.sku},${it.marca},${it.modelo},${it.color},${it.talla},${it.stockActual},${it.stockMinimo},${it.precio},${it.costo},${it.activo}")
            }
        }
        return writeCsv(context, "inventario-monturas", rows)
    }

    private fun writeCsv(context: Context, prefix: String, content: String): File {
        val dir = File(context.cacheDir, "recetas").apply { mkdirs() }
        val out = File(dir, "$prefix-${System.currentTimeMillis()}.csv")
        out.writeText(content)
        return out
    }
}
