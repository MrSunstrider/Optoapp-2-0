package com.example.optoapp.data

/**
 * Client-side replica of local GoTrue `[auth]` when
 * `password_requirements = "lower_upper_letters_digits_symbols"` and min length 6.
 * Hosted Auth still needs the dashboard step; this is the same class set that config enables.
 */
object GoTruePasswordPolicy {
    const val MIN_LENGTH = 6
    const val REQUIREMENTS = "lower_upper_letters_digits_symbols"

    fun meets(password: String): Boolean {
        if (password.length < MIN_LENGTH) return false
        if (!password.any { it.isLowerCase() }) return false
        if (!password.any { it.isUpperCase() }) return false
        if (!password.any { it.isDigit() }) return false
        if (!password.any { !it.isLetterOrDigit() }) return false
        return true
    }
}
