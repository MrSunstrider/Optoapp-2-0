package com.example.optoapp.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Converters {
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.format(isoFormatter)

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? =
        value?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it, isoFormatter) }
}
