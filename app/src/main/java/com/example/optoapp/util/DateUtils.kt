package com.example.optoapp.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object DateUtils {
    private val localZone: ZoneId = ZoneId.systemDefault()
    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): LocalDate = LocalDate.now(localZone)

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

    fun formatLocalized(date: LocalDate): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    fun getNoonTimestamp(date: LocalDate): Long =
        date.atTime(12, 0).atZone(localZone).toInstant().toEpochMilli()
}
