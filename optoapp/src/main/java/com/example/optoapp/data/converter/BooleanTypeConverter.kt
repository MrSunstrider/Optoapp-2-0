package com.example.optoapp.data.converter

import androidx.room.TypeConverter

class BooleanTypeConverter {
    @TypeConverter
    fun fromBoolean(value: Boolean?): Int? = when (value) {
        true -> 1
        false -> 0
        null -> null
    }

    @TypeConverter
    fun toBoolean(value: Int?): Boolean? = when (value) {
        1 -> true
        0 -> false
        null -> null
        else -> null
    }
}
