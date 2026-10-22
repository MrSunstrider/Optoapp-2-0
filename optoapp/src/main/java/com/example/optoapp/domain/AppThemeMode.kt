package com.example.optoapp.domain

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun parse(raw: String?): AppThemeMode = when (raw?.trim()?.lowercase()) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }

        fun resolveDark(mode: AppThemeMode, systemDark: Boolean): Boolean = when (mode) {
            SYSTEM -> systemDark
            LIGHT -> false
            DARK -> true
        }

        // WHY: banner toggle must leave SYSTEM and lock to the opposite of what the user sees now.
        fun afterToggle(mode: AppThemeMode, systemDark: Boolean): AppThemeMode =
            if (resolveDark(mode, systemDark)) LIGHT else DARK
    }

    fun storageValue(): String = name.lowercase()
}
