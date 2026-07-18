package com.example.optoapp.data.converter

import androidx.room.TypeConverter
import java.math.BigDecimal

class BigDecimalConverters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): Double? = value?.toDouble()

    @TypeConverter
    fun toBigDecimal(value: Double?): BigDecimal? = value?.let { BigDecimal.valueOf(it) }
}
