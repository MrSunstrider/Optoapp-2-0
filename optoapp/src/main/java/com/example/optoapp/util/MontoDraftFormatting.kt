package com.example.optoapp.util

import java.util.Locale
import kotlin.math.abs

/**
 * Editable money drafts must never show a forced zero like "0.0" — that forces the
 * user to delete before typing and corrupts digits into the fractional part.
 */
object MontoDraftFormatting {
    fun formatDraft(value: Double?): String {
        if (value == null || abs(value) < 1e-9) return ""
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value)
        }
    }

    fun formatDraftFromAutofill(precio: Double): String = formatDraft(precio)

    fun parseDraft(raw: String): Double? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.replace(",", ".").toDoubleOrNull()
    }
}
