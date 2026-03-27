import java.util.Calendar
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Date

object DateUtils {
    fun localToUtcMidnight(localMillis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = localMillis
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.set(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            0, 0, 0
        )
        utcCalendar.set(Calendar.MILLISECOND, 0)
        return utcCalendar.timeInMillis
    }

    fun utcMidnightToLocal(utcMillis: Long): Long {
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.timeInMillis = utcMillis
        val localCalendar = Calendar.getInstance()
        localCalendar.set(
            utcCalendar.get(Calendar.YEAR),
            utcCalendar.get(Calendar.MONTH),
            utcCalendar.get(Calendar.DAY_OF_MONTH),
            12, 0, 0
        )
        return localCalendar.timeInMillis
    }
}

fun main() {
    TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"))
    println("Default timezone: ${TimeZone.getDefault().id}")
    
    // Simulate picking April 25, 2025 on DatePicker
    // DatePicker returns UTC midnight:
    val format = SimpleDateFormat("dd/MM/yyyy")
    format.timeZone = TimeZone.getTimeZone("UTC")
    val selectedUtcMillis = format.parse("25/04/2025").time
    
    println("Selected UTC millis (DatePicker output): \$selectedUtcMillis")
    val dateUTC = Date(selectedUtcMillis)
    val localFmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss") // local default formatter
    println("If formatted directly locally: \${localFmt.format(dateUTC)}")
    
    // Convert
    val storedLocal = DateUtils.utcMidnightToLocal(selectedUtcMillis)
    println("Stored local millis: \$storedLocal")
    val storedDate = Date(storedLocal)
    println("Formatted stored local: \${localFmt.format(storedDate)}")
    
    // Back to picker
    val toPickerUtc = DateUtils.localToUtcMidnight(storedLocal)
    println("Back to picker UTC millis: \$toPickerUtc")
    format.timeZone = TimeZone.getTimeZone("UTC")
    println("Picker reads as: \${format.format(Date(toPickerUtc))}")
}
