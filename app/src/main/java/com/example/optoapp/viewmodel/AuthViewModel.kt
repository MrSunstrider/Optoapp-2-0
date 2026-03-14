package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.SecurityManager
import com.example.optoapp.data.OptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val securityManager: SecurityManager,
    private val repository: OptoRepository
) : ViewModel() {

    private val userPin = securityManager.userPin
    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput

    fun onPinDigit(digit: String) {
        if (_pinInput.value.length < 6) _pinInput.value += digit
    }

    fun clearPin() { _pinInput.value = "" }

    suspend fun validatePin(): Boolean = _pinInput.value == userPin.first()

    fun updatePin(oldPin: String, newPin: String) = viewModelScope.launch {
        if (oldPin == userPin.first()) securityManager.savePin(newPin)
    }

    suspend fun getBackupJson(): String = com.google.gson.Gson().toJson(repository.getBackupData())

    fun restoreBackup(json: String) = viewModelScope.launch {
        try {
            val data = com.google.gson.Gson().fromJson(json, com.example.optoapp.data.BackupData::class.java)
            repository.restoreBackup(data)
        } catch (e: Exception) { e.printStackTrace() }
    }
}
