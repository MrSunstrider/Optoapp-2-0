package com.example.optoapp.ui.screens

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.RecoveryState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryScreen(
    navController: NavController,
    viewModel: AuthViewModel,
) {
    val recoveryState by viewModel.recoveryState.collectAsState()
    val focusManager = LocalFocusManager.current
    var email by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetRecoveryState()
        }
    }

    Scaffold(
        topBar = {
            OptoTopAppBar(
                title = "Recuperar cuenta",
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetRecoveryState()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (recoveryState) {
                is RecoveryState.EmailSent -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Correo enviado",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Si existe una cuenta con ese correo, vas a recibir un enlace para restablecer tu contraseña.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.resetRecoveryState()
                                navController.navigate(Route.Login.route) {
                                    popUpTo(Route.Login.route) { inclusive = true }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Volver a inicio de sesión")
                        }
                    }
                }
                is RecoveryState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = (recoveryState as RecoveryState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.resetRecoveryState() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Intentar de nuevo")
                        }
                        TextButton(
                            onClick = {
                                viewModel.resetRecoveryState()
                                navController.navigate(Route.Login.route) {
                                    popUpTo(Route.Login.route) { inclusive = true }
                                }
                            },
                        ) {
                            Text("Volver a inicio de sesión")
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Ingresá el correo con el que te registraste",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            label = { Text("Correo electrónico") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Correo") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onDone = { focusManager.clearFocus() },
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )

                        localError?.let { err ->
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                    localError = "Ingresá un correo electrónico válido"
                                    return@Button
                                }
                                localError = null
                                viewModel.sendRecoveryEmail(email)
                            },
                            enabled = email.isNotBlank() && recoveryState !is RecoveryState.Loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            if (recoveryState is RecoveryState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text(
                                    "Enviar enlace de recuperación",
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
