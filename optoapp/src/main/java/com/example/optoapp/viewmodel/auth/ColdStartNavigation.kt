package com.example.optoapp.viewmodel.auth

import com.example.optoapp.ui.navigation.Route

object ColdStartNavigation {
    fun dest(
        isAuthChecked: Boolean,
        sessionValid: Boolean,
        isLoggedIn: Boolean,
        postLoginDest: String,
    ): String {
        if (!isAuthChecked || !sessionValid || !isLoggedIn) return Route.Login.route
        return postLoginDest
    }

    fun pinStateReady(isPinRequired: Boolean?, pinHasBeenSet: Boolean?): Boolean {
        if (isPinRequired == null) return false
        if (isPinRequired && pinHasBeenSet == null) return false
        return true
    }
}
