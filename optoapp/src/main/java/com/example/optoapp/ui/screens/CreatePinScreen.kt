package com.example.optoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.data.SecurityManager
import com.example.optoapp.ui.components.OptoCard
import com.example.optoapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun CreatePinScreen(navController: NavController, viewModel: AuthViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(1) }
    var firstPin by remember { mutableStateOf("") }
    var secondPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "O",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "OptoApp",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = if (step == 1) {
                    "Crear PIN de seguridad (6 dígitos)"
                } else {
                    "Confirmar PIN"
                },
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            val displayPin = if (step == 1) firstPin else secondPin
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(SecurityManager.PIN_LENGTH) { index ->
                    val filled = index < displayPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                if (filled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            }

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OptoCard(
                shape = RoundedCornerShape(24.dp),
                elevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val numbers = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "OK"),
                    )

                    numbers.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { char ->
                                val isOk = char == "OK"
                                val isClear = char == "C"
                                Button(
                                    onClick = {
                                        errorMessage = null
                                        when (char) {
                                            "C" -> {
                                                if (step == 1) {
                                                    firstPin = ""
                                                } else {
                                                    secondPin = ""
                                                }
                                            }
                                            "OK" -> {
                                                scope.launch {
                                                    isLoading = true
                                                    if (step == 1) {
                                                        if (firstPin.length == SecurityManager.PIN_LENGTH) {
                                                            if (!SecurityManager.isValidPin(firstPin)) {
                                                                errorMessage = "PIN débil. Evita secuencias o repeticiones."
                                                                firstPin = ""
                                                            } else {
                                                                step = 2
                                                            }
                                                        }
                                                    } else {
                                                        if (secondPin == firstPin) {
                                                            viewModel.createPin(firstPin)
                                                            navController.navigate("main") {
                                                                popUpTo("create_pin") { inclusive = true }
                                                            }
                                                        } else {
                                                            errorMessage = "Los PINs no coinciden"
                                                            secondPin = ""
                                                        }
                                                    }
                                                    isLoading = false
                                                }
                                            }
                                            else -> {
                                                if (step == 1) {
                                                    if (firstPin.length < SecurityManager.PIN_LENGTH) {
                                                        firstPin += char
                                                    }
                                                } else {
                                                    if (secondPin.length < SecurityManager.PIN_LENGTH) {
                                                        secondPin += char
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(64.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when {
                                            isOk -> MaterialTheme.colorScheme.primary
                                            isClear -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        contentColor = when {
                                            isOk -> MaterialTheme.colorScheme.onPrimary
                                            isClear -> MaterialTheme.colorScheme.onError
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                ) {
                                    Text(
                                        text = char,
                                        fontSize = 24.sp,
                                        fontWeight = if (isOk || isClear) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Usa un PIN difícil de adivinar",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
