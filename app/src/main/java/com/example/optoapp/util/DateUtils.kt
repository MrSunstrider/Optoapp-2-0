package com.example.optoapp.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object DateUtils {
    /**
     * Convierte un LocalDate a milisegundos en la medianoche de la zona horaria del sistema.
     * Útil para guardar en base de datos de manera consistente.
     */
    fun dateToLong(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * Convierte milisegundos a LocalDate basándose en la zona horaria del sistema.
     * Útil para mostrar en UI o procesar lógica de días.
     */
    fun longToDate(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    /**
     * Obtiene el timestamp de hoy al mediodía (12:00 PM) para notificaciones.
     * Esto evita problemas con cambios de horario (DST).
     */
    fun getNoonTimestamp(date: LocalDate): Long {
        return date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
