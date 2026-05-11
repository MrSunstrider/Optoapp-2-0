package com.example.optoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.optoapp.data.SecurityManager
import com.example.optoapp.viewmodel.AuthViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch

@Composable
fun CreatePinScreen(navController: NavController, viewModel: AuthViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(1) }
    var firstPin by remember { mutableStateOf("") }
    var secondPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "OptoApp",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (step == 1) "Crear PIN de seguridad (6 dígitos)"
                   else "Confirmar PIN",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(32.dp))

        val displayPin = if (step == 1) firstPin else secondPin
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(SecurityManager.PIN_LENGTH) { index ->
                val filled = index < displayPin.length
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(48.dp))

        val numbers = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "OK")
        )

        numbers.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { char ->
                    Button(
                        onClick = {
                            errorMessage = null
                            when (char) {
                                "C" -> {
                                    if (step == 1) firstPin = ""
                                    else secondPin = ""
                                }
                                "OK" -> {
                                    scope.launch {
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
                            .size(80.dp)
                            .padding(4.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (char == "OK") MaterialTheme.colorScheme.tertiary
                                            else if (char == "C") MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = when (char) {
                                "OK" -> MaterialTheme.colorScheme.onTertiary
                                "C" -> MaterialTheme.colorScheme.onError
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    ) {
                        Text(text = char, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
