package com.example.optoapp.util

object InputFormatters {

    /**
     * Auto-formatea input telefónico a "XXX XXX XXX" (9 dígitos, espacios cada 3).
     * Strip no-dígitos, max 9 dígitos.
     */
    fun formatPhoneInput(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(9)
        return buildString {
            for (i in digits.indices) {
                if (i == 3 || i == 6) append(' ')
                append(digits[i])
            }
        }
    }
}
