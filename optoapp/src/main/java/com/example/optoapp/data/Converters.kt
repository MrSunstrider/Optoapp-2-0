package com.example.optoapp.data

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Converters {
    private val json = Json { ignoreUnknownKeys = true }
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = json.decodeFromString(value)

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.format(isoFormatter)

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? =
        value?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it, isoFormatter) }

}
