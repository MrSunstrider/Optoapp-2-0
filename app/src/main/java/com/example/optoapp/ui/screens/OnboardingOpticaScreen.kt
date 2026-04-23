package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.subscription.SubscriptionManager
import com.example.optoapp.viewmodel.AuthViewModel

@Composable
fun OnboardingOpticaScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var nombreOptica by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val needsOnboarding by viewModel.needsOnboarding.collectAsState()
    val isPinRequired by viewModel.isPinRequired.collectAsState(initial = false)

    LaunchedEffect(needsOnboarding) {
        if (!needsOnboarding) {
            navController.navigate(if (isPinRequired) "pin" else "main") {
                popUpTo("onboarding_optica") { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Bienvenido a OptoApp", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Crea tu óptica para empezar en modo gratuito.")
        Text("Plan inicial: Free (máximo ${SubscriptionManager.FREE_MAX_PACIENTES} pacientes).")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = nombreOptica,
            onValueChange = { nombreOptica = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre de tu óptica") },
            singleLine = true
        )
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        Button(
            onClick = {
                loading = true
                error = null
                viewModel.completeOnboardingOptica(nombreOptica) { ok, msg ->
                    loading = false
                    if (!ok) error = msg
                }
            },
            enabled = nombreOptica.isNotBlank() && !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator() else Text("Crear óptica y continuar")
        }
    }
}
