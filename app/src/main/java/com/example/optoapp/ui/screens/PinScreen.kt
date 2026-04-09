package com.example.optoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.optoapp.viewmodel.AuthViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun PinScreen(navController: NavController, viewModel: AuthViewModel = hiltViewModel()) {
    val pinInput by viewModel.pinInput.collectAsState()
    val scope = rememberCoroutineScope()
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
            text = "Ingrese su PIN de seguridad",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(32.dp))

        // PIN display (dots)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(6) { index ->
                val filled = index < pinInput.length
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
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

        // Number Pad
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
                            when (char) {
                                "C" -> {
                                    viewModel.clearPin()
                                    errorMessage = null
                                }
                                "OK" -> {
                                    scope.launch {
                                        if (viewModel.validatePin()) {
                                            // PIN correcto → Navegar directo a main
                                            // (MainActivity garantiza que hay sesión activa si estamos aquí)
                                            navController.navigate("main") {
                                                popUpTo("pin") { inclusive = true }
                                            }
                                        } else {
                                            errorMessage = "PIN incorrecto"
                                            viewModel.clearPin()
                                        }
                                    }
                                }
                                else -> {
                                    viewModel.onPinDigit(char)
                                    errorMessage = null
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
