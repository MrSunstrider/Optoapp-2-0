package com.example.optoapp.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object DateUtils {
    private val localZone: ZoneId = ZoneId.systemDefault()
    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Preferencia global de zona horaria (SaaS). 
     * Se actualiza desde el SessionManager al iniciar el app o cambiar la config.
     */
    var userPreferredZone: ZoneId? = null

    fun today(): LocalDate {
        // 1. Si el usuario eligió una zona manual, manda esa.
        // 2. Si no, usamos ZoneId.systemDefault() que es el estándar moderno de Java/Android.
        val zoneId = userPreferredZone ?: java.time.ZoneId.systemDefault()
        val now = LocalDate.now(zoneId)
        android.util.Log.d("DATE_DEBUG", "today() -> Fecha: $now | Zona: ${zoneId.id} | Es Manual: ${userPreferredZone != null}")
        return now
    }

    /**
     * Conversión hacia/desde [androidx.compose.material3.DatePicker]: el valor es epoch del **inicio del día en UTC**
     * para esa fecha de calendario. Si se usa [ZoneId.systemDefault] al interpretar los millis, en zonas UTC−X
     * el día local queda un día **antes** que el elegido en el selector.
     */
    fun localDateToPickerMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun pickerMillisToLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    fun toIso(date: LocalDate): String = date.format(isoFormatter)

    fun fromIso(value: String): LocalDate = LocalDate.parse(value, isoFormatter)

    private val displayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun fromDisplayFormat(value: String): LocalDate? =
        runCatching { LocalDate.parse(value, displayFormatter) }.getOrNull()

    /**
     * Auto-formatea input del usuario a dd/MM/yyyy.
     * Inserta '/' automáticamente después de 2 dígitos (día) y 4 dígitos (mes).
     * Max 10 caracteres (dd/MM/yyyy completo).
     */
    fun formatDateInput(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(8)
        return buildString {
            for (i in digits.indices) {
                if (i == 2 || i == 4) append('/')
                append(digits[i])
            }
        }
    }

    /** Strip formatting slashes — returns only digits */
    fun digitsOnly(formatted: String): String {
        return formatted.filter { it.isDigit() }
    }

    fun formatLocalized(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-PE"))
        return date.format(formatter)
    }

    fun getNoonTimestamp(date: LocalDate): Long =
        date.atTime(12, 0).atZone(localZone).toInstant().toEpochMilli()
}
