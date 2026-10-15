package com.example.optoapp.util

import java.time.YearMonth
import javax.inject.Inject

class CurrentMonthProvider @Inject constructor() {
    fun current(): YearMonth = YearMonth.from(DateUtils.today())
}
