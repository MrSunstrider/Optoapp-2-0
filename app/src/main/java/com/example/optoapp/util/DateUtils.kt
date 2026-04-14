package com.example.optoapp.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object DateUtils {
    private val localZone: ZoneId = ZoneId.systemDefault()
    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): LocalDate = LocalDate.now(localZone)

    fun localDateToPickerMillis(date: LocalDate): Long =
        date.atStartOfDay(localZone).toInstant().toEpochMilli()

    fun pickerMillisToLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(localZone).toLocalDate()

    fun toIso(date: LocalDate): String = date.format(isoFormatter)

    fun fromIso(value: String): LocalDate = LocalDate.parse(value, isoFormatter)

    fun formatLocalized(date: LocalDate): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    fun getNoonTimestamp(date: LocalDate): Long =
        date.atTime(12, 0).atZone(localZone).toInstant().toEpochMilli()
}
