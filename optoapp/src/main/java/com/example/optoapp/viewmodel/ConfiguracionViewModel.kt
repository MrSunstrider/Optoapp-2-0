package com.example.optoapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfiguracionViewModel @Inject constructor(
    private val securityManager: SecurityManager,
) : ViewModel() {

    var pinActual by mutableStateOf("")
    var nuevoPin by mutableStateOf("")
    var confirmPin by mutableStateOf("")

    var labNombre by mutableStateOf("")
    var labContacto by mutableStateOf("")

    var dialogMessage by mutableStateOf<String?>(null)

    val availableTimeZones = listOf(
        "Detectar automáticamente",
        "America/Lima", "America/Bogota", "America/Mexico_City",
        "America/Santiago", "America/Argentina/Buenos_Aires",
        "America/Guayaquil", "America/Caracas", "Europe/Madrid",
    )

    fun initLabFields(nombre: String, contacto: String) {
        labNombre = nombre
        labContacto = contacto
    }

    fun updatePin() {
        if (nuevoPin == confirmPin && SecurityManager.isValidPin(nuevoPin)) {
            viewModelScope.launch {
                val currentPin = securityManager.userPin.first()
                if (pinActual != currentPin) {
                    dialogMessage = "El PIN actual no coincide"
                    return@launch
                }
                securityManager.savePin(nuevoPin)
                pinActual = ""
                nuevoPin = ""
                confirmPin = ""
                dialogMessage = "PIN actualizado correctamente"
            }
        } else {
            dialogMessage = "El nuevo PIN no es válido o no coincide"
        }
    }

    fun dismissDialog() {
        dialogMessage = null
    }
}
