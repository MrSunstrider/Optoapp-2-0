package com.example.optoapp.data

/**
 * Valores enviados a Supabase cuando el modelo local permite vacíos pero Postgres tiene
 * NOT NULL / `*_not_blank_chk` / FK UUID (PostgREST no debe recibir "" en columnas UUID opcionales).
 *
 * Mantener aquí la única fuente de verdad para sync y pantallas que persistan la misma entidad.
 */
object FinanzasRemoteDefaults {
    const val OPTICA_ID_FALLBACK = "mi_optica_base"

    object ServicioExtra {
        /** Fila agregada: el detalle por método va en [Pago]; la columna remota no puede ir en blanco. */
        const val METODO_PAGO_ROW = "Sin especificar"
        const val DESCRIPCION_VACIA = "Sin descripción"
        const val OT_VACIA = "-"
        const val ESTADO_VACIO = "Pendiente"
    }

    object Pago {
        /** Misma convención que [com.example.optoapp.ui.components.AbonoDialog] al crear abonos. */
        const val METODO_PAGO_VACIO = "Efectivo"
        const val TIPO_VACIO = "Abono"
    }

    object Messages {
        const val MONTO_TOTAL_INVALIDO = "Define primero un monto total válido."
        const val MONTO_TOTAL_MAYOR_A_CERO = "El monto total debe ser mayor a 0."
        const val ABONO_MAYOR_A_CERO = "Todos los abonos deben ser mayores a 0."
        const val ABONO_MAYOR_QUE_TOTAL = "El pago no puede ser mayor al total."
    }
}
