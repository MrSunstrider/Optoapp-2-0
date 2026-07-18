package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.RecoveryState

private const val WEAK_PASSWORD_ERROR = "Debe tener al menos 6 caracteres, una mayúscula, una minúscula, un número y un símbolo."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPasswordScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    val recoveryState by viewModel.recoveryState.collectAsState()
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    // Guard: if we somehow got here without a recovery link, go back
    // PasswordUpdated se permite para mostrar el mensaje de éxito
    LaunchedEffect(Unit) {
        if (recoveryState !is RecoveryState.LinkReceived && recoveryState !is RecoveryState.Loading
            && recoveryState !is RecoveryState.Error && recoveryState !is RecoveryState.PasswordUpdated) {
            navController.navigate("login") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetRecoveryState()
        }
    }

    fun validatePassword(): String? = when {
        newPassword.length < 6 -> WEAK_PASSWORD_ERROR
        !newPassword.any { it.isLowerCase() } -> WEAK_PASSWORD_ERROR
        !newPassword.any { it.isUpperCase() } -> WEAK_PASSWORD_ERROR
        !newPassword.any { it.isDigit() } -> WEAK_PASSWORD_ERROR
        !newPassword.any { !it.isLetterOrDigit() } -> WEAK_PASSWORD_ERROR
        newPassword != confirmPassword -> "Las contraseñas no coinciden."
        else -> null
    }

    Scaffold(
        topBar = {
            OptoTopAppBar(
                title = "Nueva contraseña",
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetRecoveryState()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (recoveryState) {
                is RecoveryState.PasswordUpdated -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Contraseña actualizada",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tu contraseña se actualizó correctamente. Ahora podés iniciar sesión.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.resetRecoveryState()
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Volver a iniciar sesión")
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = (recoveryState as RecoveryState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.resetRecoveryState() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Solicitar uno nuevo")
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Elegí una contraseña nueva para tu cuenta",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it; localError = null },
                            label = { Text("Nueva contraseña") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Bloqueado") },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff
                                                      else Icons.Default.Visibility,
                                        contentDescription = if (showPassword) "Ocultar" else "Mostrar"
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None
                                                   else PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; localError = null },
                            label = { Text("Confirmar contraseña") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Bloqueado") },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff
                                                      else Icons.Default.Visibility,
                                        contentDescription = if (showPassword) "Ocultar" else "Mostrar"
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None
                                                   else PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onDone = {
                                    val error = validatePassword()
                                    if (error != null) {
                                        localError = error
                                    } else {
                                        localError = null
                                        viewModel.updatePassword(newPassword)
                                    }
                                }
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        localError?.let { err ->
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val error = validatePassword()
                                if (error != null) {
                                    localError = error
                                    return@Button
                                }
                                localError = null
                                viewModel.updatePassword(newPassword)
                            },
                            enabled = newPassword.isNotBlank() && confirmPassword.isNotBlank()
                                    && recoveryState !is RecoveryState.Loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (recoveryState is RecoveryState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    "Guardar contraseña",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
