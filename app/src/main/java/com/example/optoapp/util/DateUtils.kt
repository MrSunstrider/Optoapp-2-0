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

    fun today(): LocalDate {
        val now = LocalDate.now(ZoneId.systemDefault())
        android.util.Log.d("DATE_DEBUG", "DateUtils.today() llamado. Fecha actual: $now en zona: ${ZoneId.systemDefault()}")
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

    fun formatLocalized(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("es", "PE"))
        return date.format(formatter)
    }

    fun getNoonTimestamp(date: LocalDate): Long =
        date.atTime(12, 0).atZone(localZone).toInstant().toEpochMilli()
}
