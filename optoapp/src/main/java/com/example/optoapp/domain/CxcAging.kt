package com.example.optoapp.domain

import java.time.YearMonth

data class AgingBucket(
    val labelKey: AgingBucketKey,
    val saldo: Double,
    val cantidad: Int,
)

enum class AgingBucketKey {
    D0_30,
    D31_60,
    D61_90,
    D90_PLUS,
}

data class CxcAgingResumen(
    val buckets: List<AgingBucket>,
    val dsoIlustrativo: Double?,
    val diasPromedioPonderado: Double?,
    val vacio: Boolean,
)

object CxcAging {
    fun from(
        deudores: List<Deudor>,
        @Suppress("UNUSED_PARAMETER") saldoPendiente: Double,
        ventasMes: Double,
        mes: YearMonth,
    ): CxcAgingResumen {
        if (deudores.isEmpty()) {
            return CxcAgingResumen(
                buckets = emptyBuckets(),
                dsoIlustrativo = null,
                diasPromedioPonderado = null,
                vacio = true,
            )
        }
        val groups = AgingBucketKey.entries.associateWith { mutableListOf<Deudor>() }
        for (d in deudores) {
            groups.getValue(bucketFor(d.diasDeuda)).add(d)
        }
        val buckets = AgingBucketKey.entries.map { key ->
            val rows = groups.getValue(key)
            AgingBucket(
                labelKey = key,
                saldo = rows.sumOf { it.saldo },
                cantidad = rows.size,
            )
        }
        val saldoTotal = deudores.sumOf { it.saldo }
        val diasPonderado = if (saldoTotal > 0.0) {
            deudores.sumOf { it.saldo * it.diasDeuda } / saldoTotal
        } else {
            null
        }
        val diasMes = mes.lengthOfMonth().toDouble()
        // DSO numerator must match bucket saldos (deudores), not a separate analisis.saldoPendiente.
        val dso = if (ventasMes > 0.0) {
            saldoTotal / (ventasMes / diasMes)
        } else {
            null
        }
        return CxcAgingResumen(
            buckets = buckets,
            dsoIlustrativo = dso,
            diasPromedioPonderado = diasPonderado,
            vacio = false,
        )
    }

    private fun bucketFor(dias: Int): AgingBucketKey = when {
        dias <= 30 -> AgingBucketKey.D0_30
        dias <= 60 -> AgingBucketKey.D31_60
        dias <= 90 -> AgingBucketKey.D61_90
        else -> AgingBucketKey.D90_PLUS
    }

    private fun emptyBuckets(): List<AgingBucket> =
        AgingBucketKey.entries.map { AgingBucket(it, 0.0, 0) }
}
