package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.domain.observer.MembershipObserver
import com.example.optoapp.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@Composable
fun SinOpticaScreen(
    navController: NavController,
    observer: MembershipObserver,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val userEmail by viewModel.userEmail.collectAsState(initial = "")
    val isPinRequired by viewModel.isPinRequired.collectAsState(initial = false)
    var waitingMode by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var noMembershipYet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (waitingMode) {
        LaunchedEffect(Unit) {
            observer.observeNewMembership()
                .catch { /* realtime unavailable — user can still verify manually */ }
                .collect {
                    val count = viewModel.refreshMembershipsForWaitScreen()
                    navigateAfterMembership(navController, count, isPinRequired ?: false)
                }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!waitingMode) {
                Text(
                    text = "¿Cómo vas a usar OptoApp?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                OutlinedButton(
                    onClick = { navController.navigate("onboarding_optica") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                ) {
                    Icon(Icons.Default.Business, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Soy dueño / administrador", fontWeight = FontWeight.SemiBold)
                        Text("Crear mi óptica", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { waitingMode = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Soy empleado", fontWeight = FontWeight.SemiBold)
                        Text("Esperar a que me agreguen", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                CircularProgressIndicator()
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Esperando que te agreguen a una óptica",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Pedile a tu administrador que te agregue con este email desde Configuración → Usuarios:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(24.dp))
                if (noMembershipYet) {
                    Text(
                        text = "Aún no fuiste agregado. Pedile al administrador que use exactamente este email.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Button(
                    onClick = {
                        checking = true
                        noMembershipYet = false
                        scope.launch {
                            val count = viewModel.refreshMembershipsForWaitScreen()
                            if (count == 0) {
                                noMembershipYet = true
                            } else {
                                navigateAfterMembership(navController, count, isPinRequired ?: false)
                            }
                            checking = false
                        }
                    },
                    enabled = !checking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (checking) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Ya me agregaron, verificar")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { waitingMode = false; noMembershipYet = false }) {
                    Text("Volver")
                }
            }
        }
    }
}

private fun navigateAfterMembership(navController: NavController, count: Int, isPinRequired: Boolean) {
    when {
        count == 1 -> navController.navigate(if (isPinRequired) "pin" else "main") {
            popUpTo("sin_optica") { inclusive = true }
        }
        count > 1 -> navController.navigate("seleccion_optica") {
            popUpTo("sin_optica") { inclusive = true }
        }
    }
}
