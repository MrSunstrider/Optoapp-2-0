package com.example.optoapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
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
    var loginLocalError by remember { mutableStateOf<String?>(null) }

    val isPinRequired by viewModel.isPinRequired.collectAsState(initial = false)
    val isLoggedIn by viewModel.isLoggedIn.collectAsState(initial = false)

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

        if (rememberAccount) {
            viewModel.saveRememberedEmail(email)
            viewModel.saveRememberedPassword(password)
        } else {
            viewModel.clearRememberedEmail()
            viewModel.clearRememberedPassword()
        }

        if (needsOnboarding) {
            navController.navigate("sin_optica") {
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
            .statusBarsPadding()
            .testTag(TestTags.LOGIN_SCREEN_ROOT)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Spacer(modifier = Modifier.height(40.dp))

            val blurRadius = remember { Animatable(50f) }
            LaunchedEffect(Unit) {
                blurRadius.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 770, easing = FastOutSlowInEasing)
                )
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically { -40 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(com.example.optoapp.R.drawable.logo_login),
                        contentDescription = "OptoApp",
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .blur(radiusX = blurRadius.value.dp, radiusY = blurRadius.value.dp)
                    )
                    Text(
                        text = "Optoapp",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Sistema de gestión óptica",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "v${com.example.optoapp.BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OptoCard(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = { Text("Correo electrónico") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Icono de correo electrónico")
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

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Icono de contraseña")
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag(TestTags.LOGIN_REMEMBER_ACCOUNT_CHECK)
                        ) {
                            Checkbox(
                                checked = rememberAccount,
                                onCheckedChange = { rememberAccount = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Recordar Cuenta",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { navController.navigate("recovery") },
                            modifier = Modifier.testTag(TestTags.LOGIN_OLVIDASTE_BTN)
                        ) {
                            Text(
                                text = "¿Olvidaste tu contraseña?",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

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
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    loginLocalError?.let { err ->
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                loginLocalError = "Ingresa un correo electrónico válido"
                                return@Button
                            }
                            loginLocalError = null
                            viewModel.login(email, password)
                        },
                        enabled  = email.isNotBlank() && password.isNotBlank()
                                && authState !is AuthState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
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
                                style = MaterialTheme.typography.bodyLarge,
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
                            contentDescription = "Logo de Google",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Continuar con Google",
                            fontWeight = FontWeight.Medium
                        )
                    }

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

            Text(
                text = "¿Problemas con tu cuenta? Contacta al administrador de tu óptica.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
