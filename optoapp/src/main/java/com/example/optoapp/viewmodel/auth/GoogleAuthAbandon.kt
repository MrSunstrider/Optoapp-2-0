package com.example.optoapp.viewmodel.auth

import com.example.optoapp.viewmodel.AuthState

object GoogleAuthAbandon {
    fun nextState(current: AuthState): AuthState =
        if (current is AuthState.Loading) AuthState.Idle else current
}
