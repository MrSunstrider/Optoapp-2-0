package com.example.optoapp.ui.screens

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.optoapp.OptoApplication
import com.example.optoapp.viewmodel.OptoViewModel
import com.example.optoapp.viewmodel.OptoViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(@Suppress("UNUSED_PARAMETER") navController: NavController, drawerState: DrawerState) {
    val context = LocalContext.current
    val app = context.applicationContext as OptoApplication
    val viewModel: OptoViewModel = viewModel(
        factory = OptoViewModelFactory(app.repository, app.securityManager)
    )
    val scope = rememberCoroutineScope()
    
    var pinActual by remember { mutableStateOf("") }
    var nuevoPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    
    var showDialog by remember { mutableStateOf(false) }
    var dialogMsg by remember { mutableStateOf("") }

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
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ADVERTENCIA: Restaurar un respaldo reemplazará todos los datos actuales.", fontSize = 12.sp, color = Color.Red)
                    }
                    
                    OutlinedButton(
                        onClick = { restoreBackupLauncher.launch("application/json") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
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
