package com.example.optoapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.testing.TestTags
import com.example.optoapp.viewmodel.AuthState
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.components.OptoCard

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val pendingMemberships by viewModel.pendingMemberships.collectAsState()
    val needsOnboarding by viewModel.needsOnboarding.collectAsState()
    val focusManager = LocalFocusManager.current

    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var showPassword    by remember { mutableStateOf(false) }
    var rememberAccount by remember { mutableStateOf(false) }

    val isPinRequired by viewModel.isPinRequired.collectAsState(initial = false)
    val isLoggedIn by viewModel.isLoggedIn.collectAsState(initial = false)

    // Cargar email y contraseña recordados al iniciar la pantalla
    LaunchedEffect(Unit) {
        val savedEmail = viewModel.getRememberedEmail()
        if (savedEmail.isNotBlank()) {
            email = savedEmail
            rememberAccount = true
            val savedPassword = viewModel.getRememberedPassword()
            if (savedPassword.isNotBlank()) {
                password = savedPassword
            }
        }
    }

    LaunchedEffect(authState, pendingMemberships, isLoggedIn, needsOnboarding) {
        if (authState !is AuthState.Success) return@LaunchedEffect

        // Recordar cuenta: guardar o limpiar email y contraseña según el checkbox
        if (rememberAccount) {
            viewModel.saveRememberedEmail(email)
            viewModel.saveRememberedPassword(password)
        } else {
            viewModel.clearRememberedEmail()
            viewModel.clearRememberedPassword()
        }

        if (needsOnboarding) {
            navController.navigate("onboarding_optica") {
                popUpTo("login") { inclusive = true }
            }
            return@LaunchedEffect
        }
        if (pendingMemberships.isNotEmpty()) {
            navController.navigate("seleccion_optica") {
                popUpTo("login") { inclusive = true }
            }
            return@LaunchedEffect
        }
        if (!isLoggedIn) return@LaunchedEffect
        val dest = if (isPinRequired == true) "pin" else "main"
        navController.navigate(dest) {
            popUpTo("login") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.LOGIN_SCREEN_ROOT)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ─── Logo / Encabezado ────────────────────────────────────────────
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically { -40 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Logo: ojo dentro de círculo (emula la "O")
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "OptoApp",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "OptoApp",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Clinical Software 2026",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "v${com.example.optoapp.BuildConfig.VERSION_NAME}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── Card del formulario ──────────────────────────────────────────
            OptoCard(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = { Text("Correo electrónico") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction    = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier   = Modifier.fillMaxWidth().testTag(TestTags.LOGIN_EMAIL_FIELD),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Contraseña
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (email.isNotBlank() && password.isNotBlank()) {
                                    viewModel.login(email, password)
                                }
                            }
                        ),
                        singleLine = true,
                        modifier   = Modifier.fillMaxWidth().testTag(TestTags.LOGIN_PASSWORD_FIELD),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // ─── Recordar Cuenta ───────────────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.LOGIN_REMEMBER_ACCOUNT_CHECK)
                    ) {
                        Checkbox(
                            checked = rememberAccount,
                            onCheckedChange = { rememberAccount = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Recordar Cuenta",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // ─── Error message ────────────────────────────────────────
                    AnimatedVisibility(
                        visible = authState is AuthState.Error,
                        enter   = fadeIn(),
                        exit    = fadeOut()
                    ) {
                        if (authState is AuthState.Error) {
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag(TestTags.LOGIN_ERROR_MESSAGE)
                            ) {
                                Text(
                                    text = (authState as AuthState.Error).message,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // ─── Botón Entrar al Sistema ──────────────────────────────
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.login(email, password)
                        },
                        enabled  = email.isNotBlank() && password.isNotBlank()
                                && authState !is AuthState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag(TestTags.LOGIN_INGRESAR_BTN),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = "ENTRAR AL SISTEMA",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.loginWithGoogle()
                        },
                        enabled = authState !is AuthState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(com.example.optoapp.R.drawable.ic_google_logo),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Continuar con Google",
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // ─── Botón Crear Cuenta ───────────────────────────────────
                    OutlinedButton(
                        onClick = { navController.navigate("register") },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Crear cuenta con correo electrónico",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ─── Pie ──────────────────────────────────────────────────────────
            Text(
                text = "Si ya tienes cuenta, contacta al administrador de tu óptica.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
