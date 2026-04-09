package com.example.optoapp.ui.screens

import android.content.Context

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(@Suppress("UNUSED_PARAMETER") navController: NavController, drawerState: DrawerState, viewModel: AuthViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var pinActual by remember { mutableStateOf("") }
    var nuevoPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    
    var showDialog by remember { mutableStateOf(false) }
    var dialogMsg by remember { mutableStateOf("") }
    
    val isPinRequired by viewModel.isPinRequired.collectAsState(initial = true)
    
    val sharedPreferences = context.getSharedPreferences("optoapp_prefs", Context.MODE_PRIVATE)
    var confirmReminders by remember { mutableStateOf(sharedPreferences.getBoolean("pref_enable_reminders", true)) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val json = viewModel.getBackupJson()
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(json.toByteArray())
                }
                dialogMsg = "Respaldo exportado exitosamente."
                showDialog = true
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val inputStream = context.contentResolver.openInputStream(it)
                val json = inputStream?.bufferedReader()?.use { it.readText() }
                if (json != null) {
                    viewModel.restoreBackup(json)
                    dialogMsg = "Base de datos restaurada correctamente."
                    showDialog = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Seguridad
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Seguridad y Acceso", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    // Nuevo Switch para PIN Opcional
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Requerir PIN al inicio", fontSize = 16.sp)
                            Text(
                                "Solicita el PIN de seguridad cada vez que abres la app.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isPinRequired,
                            onCheckedChange = { viewModel.togglePinRequired(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    OutlinedTextField(value = pinActual, onValueChange = { pinActual = it }, label = { Text("PIN Actual") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = nuevoPin, onValueChange = { nuevoPin = it }, label = { Text("Nuevo PIN") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = confirmPin, onValueChange = { confirmPin = it }, label = { Text("Confirmar Nuevo PIN") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    
                    Button(
                        onClick = {
                            if (nuevoPin == confirmPin && nuevoPin.length >= 4) {
                                scope.launch {
                                    viewModel.updatePin(pinActual, nuevoPin)
                                    dialogMsg = "PIN actualizado correctamente."
                                    showDialog = true
                                    pinActual = ""; nuevoPin = ""; confirmPin = ""
                                }
                            } else {
                                dialogMsg = "Los PINs no coinciden o son inválidos."
                                showDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Actualizar PIN")
                    }
                }
            }
            
            // Preferencias
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ajustes Generales", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recordatorios Automáticos", fontSize = 16.sp)
                            Text(
                                "Programar notificaciones a las 12:00 pm para citas agendadas.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = confirmReminders,
                            onCheckedChange = { isChecked ->
                                confirmReminders = isChecked
                                sharedPreferences.edit().putBoolean("pref_enable_reminders", isChecked).apply()
                            }
                        )
                    }
                }
            }
            
            // Gestión de Datos
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Gestión de Datos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Realiza respaldos de tu información local para evitar pérdidas.", fontSize = 14.sp)
                    
                    Button(
                        onClick = { createBackupLauncher.launch("OptoApp_Backup_${System.currentTimeMillis()}.json") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Descargar Respaldo Total")
                    }
                    
                    HorizontalDivider()
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ADVERTENCIA: Restaurar un respaldo reemplazará todos los datos actuales.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                    
                    OutlinedButton(
                        onClick = { restoreBackupLauncher.launch("application/json") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Restaurar Respaldo")
                    }
                }
            }
        }
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text("OK") } },
            title = { Text("Información") },
            text = { Text(dialogMsg) }
        )
    }
}
