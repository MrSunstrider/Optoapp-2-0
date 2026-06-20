package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.subscription.PlanCode
import com.example.optoapp.subscription.SubscriptionManager
import com.example.optoapp.subscription.SubscriptionTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    val tier: StateFlow<SubscriptionTier> = subscriptionManager.tier
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubscriptionTier.FREE)

    val planCode: StateFlow<PlanCode> = subscriptionManager.planCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanCode.FREE)

    val devProOverride: StateFlow<Boolean> = subscriptionManager.devProOverride
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val pacienteCount = sessionManager.opticaId.flatMapLatest { oid ->
        repository.countPacientesForOptica(oid)
    }

    val canAddPaciente: StateFlow<Boolean> = combine(
        subscriptionManager.tier,
        pacienteCount
    ) { t, count ->
        count < subscriptionManager.maxPacientes(t)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun refreshPlanFromServer() = viewModelScope.launch {
        val oid = sessionManager.opticaId.first()
        subscriptionManager.refreshPlanFromServer(oid)
    }

    fun setDevProOverride(enabled: Boolean) = viewModelScope.launch {
        subscriptionManager.setDevProOverride(enabled)
    }

    fun launchProPurchase(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            subscriptionManager.setProFromLocalCache()
            onSuccess()
        }
    }
}
