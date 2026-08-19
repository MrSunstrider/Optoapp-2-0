package com.example.optoapp.viewmodel.auth

import com.example.optoapp.ui.navigation.Route

object PostLoginNavigation {
    fun dest(
        count: Int,
        needsOnboarding: Boolean,
        fetchError: Boolean,
        isPinRequired: Boolean,
        @Suppress("UNUSED_PARAMETER") pinHasBeenSet: Boolean,
    ): String {
        if (fetchError) return ""
        if (count > 1) return Route.SeleccionOptica.route
        if (count == 0 && needsOnboarding) return Route.SinOptica.route
        return if (isPinRequired) Route.Pin.route else Route.Main.route
    }
}
