package com.example.optoapp.viewmodel.auth

import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.viewmodel.RecoveryState

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

    /** Mirror session-invalidation: keep NewPassword when recovery owns the stack. */
    fun recoveryBlocksColdStartRestore(recoveryState: RecoveryState): Boolean =
        recoveryState is RecoveryState.LinkReceived ||
            recoveryState is RecoveryState.PasswordUpdated

    fun pinStateReady(isPinRequired: Boolean?, pinHasBeenSet: Boolean?): Boolean {
        if (isPinRequired == null) return false
        if (isPinRequired && pinHasBeenSet == null) return false
        return true
    }
}
