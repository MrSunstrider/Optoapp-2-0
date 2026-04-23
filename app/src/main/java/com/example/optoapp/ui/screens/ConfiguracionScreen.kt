package com.example.optoapp.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.optoapp.billing.PlayBillingManager
import com.example.optoapp.subscription.SubscriptionTier
import com.example.optoapp.viewmodel.SubscriptionViewModel
import com.example.optoapp.viewmodel.SyncDiagnosticsViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.BuildConfig
import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.SecurityManager
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.LaboratorioConfigViewModel
import com.example.optoapp.viewmodel.RoleManagementViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(
    @Suppress("UNUSED_PARAMETER") navController: NavController,
    drawerState: DrawerState,
    viewModel: AuthViewModel = hiltViewModel(),
    laboratorioVm: LaboratorioConfigViewModel = hiltViewModel(),
    subscriptionVm: SubscriptionViewModel = hiltViewModel(),
    syncDiagVm: SyncDiagnosticsViewModel = hiltViewModel(),
    roleVm: RoleManagementViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val labUi by laboratorioVm.uiState.collectAsState()
    var labNombre by remember { mutableStateOf("") }
    var labContacto by remember { mutableStateOf("") }
    LaunchedEffect(labUi.opticaId, labUi.laboratorioNombre, labUi.laboratorioContacto) {
        labNombre = labUi.laboratorioNombre
        labContacto = labUi.laboratorioContacto
    }
    LaunchedEffect(Unit) {
        laboratorioVm.syncFromServer()
        subscriptionVm.refreshPlanFromServer()
    }

    val subTier by subscriptionVm.tier.collectAsState()
    val devProOverride by subscriptionVm.devProOverride.collectAsState()
    val syncErrors by syncDiagVm.errorRows.collectAsState()
    val roleUi by roleVm.uiState.collectAsState()
    val opticaRol by viewModel.opticaRol.collectAsState(initial = "admin")
    val canManageUsers = AppRoles.canManageUsers(opticaRol)
    val canAssignAdminRole = opticaRol.trim().equals("admin", ignoreCase = true)
    val allowedRoles = remember(canAssignAdminRole) {
        if (canAssignAdminRole) {
            listOf("especialista", "asesor", "asesora", "ventas", "invitado", "gerente", "admin")
        } else {
            listOf("especialista", "asesor", "asesora", "ventas", "invitado", "gerente")
        }
    }
    
    var pinActual by remember { mutableStateOf("") }
    var nuevoPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    
    var showDialog by remember { mutableStateOf(false) }
    var dialogMsg by remember { mutableStateOf("") }
    
    val isPinRequired by viewModel.isPinRequired.collectAsState(initial = true)
    
    val sharedPreferences = context.getSharedPreferences("optoapp_prefs", Context.MODE_PRIVATE)
    var confirmReminders by remember { mutableStateOf(sharedPreferences.getBoolean("pref_enable_reminders", true)) }

    LaunchedEffect(canManageUsers) {
        if (canManageUsers) roleVm.loadMembers()
    }
    LaunchedEffect(allowedRoles, roleUi.roleInput) {
        if (roleUi.roleInput !in allowedRoles) {
            roleVm.updateRole(allowedRoles.first())
        }
    }

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
                    viewModel.restoreBackup(json) { msg ->
                        dialogMsg = msg
                        showDialog = true
                    }
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

                    OutlinedTextField(
                        value = pinActual,
                        onValueChange = { pinActual = it.filter { c -> c.isDigit() }.take(SecurityManager.PIN_LENGTH) },
                        label = { Text("PIN actual (${SecurityManager.PIN_LENGTH} dígitos)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = nuevoPin,
                        onValueChange = { nuevoPin = it.filter { c -> c.isDigit() }.take(SecurityManager.PIN_LENGTH) },
                        label = { Text("Nuevo PIN (${SecurityManager.PIN_LENGTH} dígitos)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it.filter { c -> c.isDigit() }.take(SecurityManager.PIN_LENGTH) },
                        label = { Text("Confirmar nuevo PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Button(
                        onClick = {
                            if (nuevoPin == confirmPin && nuevoPin.length == SecurityManager.PIN_LENGTH) {
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
                                "Aviso el día de la cita alrededor de las 12:00 (hora del teléfono). Si guardas la cita ese mismo día después de las 12:00, se envía un aviso en segundos. Activa las notificaciones del sistema para OptoApp.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = confirmReminders,
                            onCheckedChange = { isChecked ->
                                confirmReminders = isChecked
                                sharedPreferences.edit(commit = false) {
                                    putBoolean("pref_enable_reminders", isChecked)
                                }
                            }
                        )
                    }
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Laboratorio (esta óptica)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Nombre y WhatsApp o teléfono del laboratorio. Se usa para enviar el ticket de orden con un toque.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OptoTextField(
                        value = labNombre,
                        onValueChange = { labNombre = it },
                        label = "Nombre del laboratorio (opcional)"
                    )
                    OptoTextField(
                        value = labContacto,
                        onValueChange = { labContacto = it },
                        label = "WhatsApp / teléfono del laboratorio",
                        keyboardType = KeyboardType.Phone
                    )
                    Button(
                        onClick = { laboratorioVm.save(labNombre, labContacto) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar contacto de laboratorio")
                    }
                }
            }

            if (canManageUsers) {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Usuarios y roles", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Asigna rol por email a cuentas ya registradas. Si el email no existe, primero debe crear su cuenta en login.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OptoTextField(
                            value = roleUi.emailInput,
                            onValueChange = roleVm::updateEmail,
                            label = "Email de la cuenta"
                        )
                        DropdownField(
                            label = "Rol",
                            selected = roleUi.roleInput,
                            options = allowedRoles,
                            onSelected = roleVm::updateRole
                        )
                        if (!canAssignAdminRole) {
                            Text(
                                "Solo admin puede asignar el rol admin.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { roleVm.assignRole() },
                                enabled = !roleUi.loading
                            ) { Text("Asignar / actualizar rol") }
                            OutlinedButton(
                                onClick = { roleVm.loadMembers() },
                                enabled = !roleUi.loading
                            ) { Text("Actualizar lista") }
                        }
                        roleUi.message?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp) }
                        roleUi.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                        if (roleUi.members.isEmpty()) {
                            Text("No hay miembros visibles en esta óptica.", fontSize = 12.sp)
                        } else {
                            roleUi.members.take(20).forEach { row ->
                                Text("• ${row.email.ifBlank { row.userId }} — ${row.rol}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Suscripción y límites", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Plan: ${if (subTier == SubscriptionTier.PRO) "PRO (ilimitado)" else "Gratuito (máx. ${com.example.optoapp.subscription.SubscriptionManager.FREE_MAX_PACIENTES} pacientes)"}",
                        fontSize = 14.sp
                    )
                    if (BuildConfig.DEBUG && BuildConfig.FORCE_PRO_DEV) {
                        Text(
                            "PRO forzado en esta compilación (local.properties: optoapp.dev.force_pro).",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Producto Play: ${PlayBillingManager.SUBSCRIPTION_PRODUCT_ID}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            val act = context as? Activity
                            if (act != null) {
                                subscriptionVm.launchProPurchase(act) { msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Comprar / renovar PRO (Play Store)")
                    }
                    if (BuildConfig.DEBUG) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Modo desarrollador: forzar PRO", fontSize = 14.sp)
                                Text("Solo para pruebas sin Play Billing.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = devProOverride,
                                onCheckedChange = { subscriptionVm.setDevProOverride(it) }
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { subscriptionVm.refreshPlanFromServer() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sincronizar plan desde servidor")
                    }
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Diagnóstico de sincronización", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Si algún registro no pudo subirse o bajarse respecto a la nube, se lista aquí con el detalle. " +
                            "No es un fallo de la app por sí solo: revisa conexión y vuelve a sincronizar desde el menú.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (syncErrors.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                "No hay registros con error de sincronización.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                val body = syncErrors.joinToString("\n\n") { row ->
                                    buildString {
                                        appendLine("${row.entityType} · ${row.entityId}")
                                        appendLine("Estado: ${row.status}")
                                        appendLine("Error: ${row.lastError}")
                                        append("Actualizado (ms): ${row.updatedAt}")
                                    }
                                }
                                val clip = ClipData.newPlainText("Errores sincronización OptoApp", body)
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(clip)
                                Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copiar todo (${syncErrors.size})")
                        }
                        TextButton(
                            onClick = {
                                syncDiagVm.clearErrorHistory()
                                Toast.makeText(context, "Lista de errores borrada (los datos locales no se eliminan).", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Limpiar esta lista")
                        }
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(syncErrors, key = { "${it.entityType}-${it.entityId}-${it.updatedAt}" }) { row ->
                                Text(
                                    "${row.entityType} · ${row.entityId.take(12)}… → ${row.lastError}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
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
