package com.example.optoapp.ui.navigation

// Declaration order is the tab row order of CostosYGastosScreen; the ordinal is the selected tab index.
enum class CostosTab(val arg: String) {
    GASTOS("gastos"),
    LENTES("lentes"),
    BISELADO("biselado"),
    LC("lc"),
    SERVICIOS("servicios"),
    ;

    companion object {
        const val KEY = "tab"

        // Links from older builds or typos must still land on a usable tab instead of crashing.
        fun fromArg(raw: String?): CostosTab = entries.firstOrNull { it.arg == raw } ?: GASTOS
    }
}
