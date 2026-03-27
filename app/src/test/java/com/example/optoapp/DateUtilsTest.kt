package com.example.optoapp

import com.example.optoapp.util.DateUtils
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class DateUtilsTest {
    @Test
    fun testDateOffset() {
        val out = StringBuilder()
        out.append("Default timezone: ${TimeZone.getDefault().id}\n")
        
        val format = SimpleDateFormat("dd/MM/yyyy")
        format.timeZone = TimeZone.getTimeZone("UTC")
        val selectedUtcMillis = format.parse("25/04/2025")!!.time
        
        out.append("Selected UTC millis: $selectedUtcMillis\n")
        val dateUTC = Date(selectedUtcMillis)
        val localFmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss") // local default formatter
        out.append("If formatted directly locally: ${localFmt.format(dateUTC)}\n")
        
        val storedLocal = DateUtils.dateToLong(DateUtils.longToDate(selectedUtcMillis))
        out.append("Stored local millis: $storedLocal\n")
        val storedDate = Date(storedLocal)
        out.append("Formatted stored local: ${localFmt.format(storedDate)}\n")
        
        val toPickerUtc = DateUtils.dateToLong(DateUtils.longToDate(storedLocal))
        out.append("Back to picker UTC millis: $toPickerUtc\n")
        out.append("Picker reads as: ${format.format(Date(toPickerUtc))}\n")
        
        System.out.println(out.toString())
    }
}
