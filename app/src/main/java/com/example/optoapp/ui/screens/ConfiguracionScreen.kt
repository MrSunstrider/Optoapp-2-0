package com.example.optoapp.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.optoapp.R
import com.example.optoapp.billing.PlayBillingManager
import com.example.optoapp.subscription.PlanCode
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
import com.example.optoapp.viewmodel.FiscalConfigViewModel
import com.example.optoapp.viewmodel.LaboratorioConfigViewModel
import com.example.optoapp.viewmodel.PlanManagementUiState
import com.example.optoapp.viewmodel.PlanManagementViewModel
import com.example.optoapp.viewmodel.RoleManagementViewModel
import com.example.optoapp.viewmodel.SettingsViewModel
import com.example.optoapp.viewmodel.SyncState
import com.example.optoapp.viewmodel.SyncViewModel
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.Duration
import java.time.ZoneId

private const val INTERNAL_OWNER_EMAIL = "jaermadera@gmail.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(
    @Suppress("UNUSED_PARAMETER") navController: NavController,
    drawerState: DrawerState,
    syncVm: SyncViewModel,
    viewModel: AuthViewModel = hiltViewModel(),
    fiscalVm: FiscalConfigViewModel = hiltViewModel(),
    laboratorioVm: LaboratorioConfigViewModel = hiltViewModel(),
    settingsVm: SettingsViewModel = hiltViewModel(),
    subscriptionVm: SubscriptionViewModel = hiltViewModel(),
    syncDiagVm: SyncDiagnosticsViewModel = hiltViewModel(),
    roleVm: RoleManagementViewModel = hiltViewModel(),
    planVm: PlanManagementViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fiscalUi by fiscalVm.uiState.collectAsState()
    val labUi by laboratorioVm.uiState.collectAsState()
    var labNombre by remember { mutableStateOf("") }
    var labContacto by remember { mutableStateOf("") }
    LaunchedEffect(labUi.opticaId, labUi.laboratorioNombre, labUi.laboratorioContacto) {
        labNombre = labUi.laboratorioNombre
        labContacto = labUi.laboratorioContacto
    }
    LaunchedEffect(Unit) {
        fiscalVm.syncFromServer()
        laboratorioVm.syncFromServer()
        subscriptionVm.refreshPlanFromServer()
    }
    val planCode by subscriptionVm.planCode.collectAsState()
    val devProOverride by subscriptionVm.devProOverride.collectAsState()
    val syncErrors by syncDiagVm.errorRows.collectAsState()
    val globalSyncState by syncVm.syncState.collectAsState()
    val remoteSyncTelemetry by syncDiagVm.remoteTelemetry.collectAsState()
    val remoteSyncTelemetryLoading by syncDiagVm.remoteTelemetryLoading.collectAsState()
    val remoteSyncTelemetryError by syncDiagVm.remoteTelemetryError.collectAsState()
    
    val userTimeZone by settingsVm.userTimeZone.collectAsState()
    val availableTimeZones = remember {
        listOf(
            "Detectar automáticamente",
            "America/Lima",
            "America/Bogota",
            "America/Mexico_City",
            "America/Santiago",
            "America/Argentina/Buenos_Aires",
            "America/Guayaquil",
            "America/Caracas",
            "Europe/Madrid"
        )
    }
    val roleUi by roleVm.uiState.collectAsState()
    val planUi by planVm.uiState.collectAsState()
    val opticaRol by viewModel.opticaRol.collectAsState(initial = "admin")
    val userEmail by viewModel.userEmail.collectAsState(initial = "")
    val canManageUsers = AppRoles.canManageUsers(opticaRol)
    val canManagePlans = AppRoles.canManagePlans(opticaRol)
    val canManageBackups = opticaRol.trim().equals("admin", ignoreCase = true)
    val canUseInternalPlan = userEmail.trim().equals(INTERNAL_OWNER_EMAIL, ignoreCase = true)
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
    var nuevaSucursalNombre by remember { mutableStateOf("") }
    var creatingSucursal by remember { mutableStateOf(false) }
    
    var showDialog by remember { mutableStateOf(false) }
    var dialogMsg by remember { mutableStateOf("") }

    LaunchedEffect(fiscalUi.message, fiscalUi.error) {
        when {
            !fiscalUi.message.isNullOrBlank() -> {
                dialogMsg = fiscalUi.message!!
                showDialog = true
                fiscalVm.clearMessages()
            }
            !fiscalUi.error.isNullOrBlank() -> {
                dialogMsg = fiscalUi.error!!
                showDialog = true
                fiscalVm.clearMessages()
            }
        }
    }
    
    val pinHasBeenSet by viewModel.pinHasBeenSet.collectAsState(initial = true)
    val isPinRequired by viewModel.isPinRequired.collectAsState(initial = true)
    val remindersEnabled by settingsVm.remindersEnabled.collectAsState()
    val notificationPermissionGranted =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    val systemNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

    LaunchedEffect(canManageUsers) {
        if (canManageUsers) roleVm.loadMembers()
    }
    LaunchedEffect(canManagePlans) {
        if (canManagePlans) planVm.load()
    }
    LaunchedEffect(globalSyncState) {
        when (globalSyncState) {
            is SyncState.Success, is SyncState.Error -> syncDiagVm.refreshRemoteTelemetry()
            else -> Unit
        }
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
                runCatching {
                    val json = viewModel.getBackupJson()
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(json.toByteArray())
                    }
                }.onSuccess {
                    dialogMsg = context.getString(R.string.config_backup_export_success)
                    showDialog = true
                }.onFailure { e ->
                    dialogMsg = e.message ?: context.getString(R.string.config_backup_export_failed)
                    showDialog = true
                }
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            scope.launch {
                val inputStream = context.contentResolver.openInputStream(selectedUri)
                val json = inputStream?.bufferedReader()?.use { reader -> reader.readText() }
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(stringResource(R.string.config_title)) },
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
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ─── SEGURIDAD Y ACCESO ─────────────────────────────────────────
            Text(
                text = "SEGURIDAD Y ACCESO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 2.dp, top = 8.dp)
            )

            SecuritySection(
                pinHasBeenSet = pinHasBeenSet,
                isPinRequired = isPinRequired,
                pinActual = pinActual,
                nuevoPin = nuevoPin,
                confirmPin = confirmPin,
                onPinActualChange = { pinActual = it },
                onNuevoPinChange = { nuevoPin = it },
                onConfirmPinChange = { confirmPin = it },
                onPinRequiredChange = { viewModel.togglePinRequired(it) },
                onUpdatePin = {
                    if (nuevoPin == confirmPin && SecurityManager.isValidPin(nuevoPin)) {
                        scope.launch {
                            viewModel.updatePin(pinActual, nuevoPin)
                            dialogMsg = context.getString(R.string.config_security_pin_updated)
                            showDialog = true
                            pinActual = ""; nuevoPin = ""; confirmPin = ""
                        }
                    } else {
                        dialogMsg = context.getString(R.string.config_security_pin_invalid)
                        showDialog = true
                    }
                }
            )
            
            // ─── SISTEMA ────────────────────────────────────────────────────────
            Text(
                text = "SISTEMA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 2.dp, top = 8.dp)
            )

            // Preferencias
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.config_general_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    // Selector de Zona Horaria
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Zona Horaria (SaaS)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Ajusta esto si la hora del app no coincide con tu reloj local.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        DropdownField(
                            label = "Seleccionar Ciudad/Zona",
                            selected = userTimeZone ?: "Detectar automáticamente",
                            options = availableTimeZones,
                            onSelected = { selected ->
                                if (selected == "Detectar automáticamente") settingsVm.setUserTimeZone(null)
                                else settingsVm.setUserTimeZone(selected)
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.config_general_reminders_title), fontSize = 16.sp)
                            Text(
                                stringResource(R.string.config_general_reminders_desc),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = settingsVm::setRemindersEnabled
                        )
                    }
                    val remindersStatusText = when {
                        !notificationPermissionGranted -> stringResource(R.string.config_general_reminders_state_no_permission)
                        !systemNotificationsEnabled -> stringResource(R.string.config_general_reminders_state_system_disabled)
                        !remindersEnabled -> stringResource(R.string.config_general_reminders_state_app_disabled)
                        else -> stringResource(R.string.config_general_reminders_state_active)
                    }
                    val remindersStatusColor = when {
                        !notificationPermissionGranted || !systemNotificationsEnabled -> MaterialTheme.colorScheme.error
                        !remindersEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                    Text(
                        text = stringResource(R.string.config_general_reminders_state_prefix, remindersStatusText),
                        fontSize = 12.sp,
                        color = remindersStatusColor
                    )
                    OutlinedButton(
                        onClick = {
                            settingsVm.sendTestNotification()
                            Toast.makeText(context, context.getString(R.string.config_notification_test_sent), Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.config_test_notification_action))
                    }
                }
            }

            // ─── DATOS DE LA ÓPTICA ──────────────────────────────────────────
            Text(
                text = "DATOS DE LA ÓPTICA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 2.dp, top = 8.dp)
            )

            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.config_laboratory_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.config_laboratory_section_description),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OptoTextField(
                        value = labNombre,
                        onValueChange = { labNombre = it },
                        label = stringResource(R.string.config_laboratory_name_label)
                    )
                    OptoTextField(
                        value = labContacto,
                        onValueChange = { labContacto = it },
                        label = stringResource(R.string.config_laboratory_contact_label),
                        keyboardType = KeyboardType.Phone
                    )
                    Button(
                        onClick = { laboratorioVm.save(labNombre, labContacto) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.config_laboratory_save_action))
                    }
                }
            }

            if (canManageUsers) {
                FiscalDataSection(
                    fiscalUi = fiscalUi,
                    onDraftChange = { update ->
                        fiscalVm.updateDraft(
                            nombreComercial = update.nombreComercial,
                            docTipo = update.docTipo,
                            docNumero = update.docNumero,
                            razonSocial = update.razonSocial,
                            direccionFiscal = update.direccionFiscal,
                            distritoCiudadDepartamento = update.distritoCiudadDepartamento,
                            moneda = update.moneda,
                            pais = update.pais,
                            contactoWhatsappTelefono = update.contactoWhatsappTelefono
                        )
                        fiscalVm.clearMessages()
                    },
                    onSave = fiscalVm::save
                )
            }

            if (canManagePlans) {
                PlanManagementSection(
                    planUi = planUi,
                    canUseInternalPlan = canUseInternalPlan,
                    onPlanCodeChange = planVm::updatePlanCode,
                    onPlanStatusChange = planVm::updatePlanStatus,
                    onMaxOpticasChange = planVm::updateMaxOpticas,
                    onMaxPacientesChange = planVm::updateMaxPacientes,
                    onMaxUsuariosChange = planVm::updateMaxUsuarios,
                    onApplyPreset = { planVm.applyPreset() },
                    onSave = { planVm.save() },
                    onReload = { planVm.load() }
                )
            }

            if (canManageUsers) {
                ClinicalIntegritySection(
                    onResolveDuplicates = {
                        viewModel.resolveDuplicateHistorias { msg ->
                            dialogMsg = msg
                            showDialog = true
                        }
                    }
                )
            }

            if (canManageUsers) {
                UsuariosRolesSection(
                    roleUi = roleUi,
                    allowedRoles = allowedRoles,
                    canAssignAdminRole = canAssignAdminRole,
                    onEmailChange = roleVm::updateEmail,
                    onRoleChange = roleVm::updateRole,
                    onAssignRole = { roleVm.assignRole() },
                    onRefresh = { roleVm.loadMembers() }
                )
            }

            if (canManageUsers) {
                SucursalesSection(
                    nuevaSucursalNombre = nuevaSucursalNombre,
                    creatingSucursal = creatingSucursal,
                    onNombreChange = { nuevaSucursalNombre = it },
                    onCreate = {
                        creatingSucursal = true
                        viewModel.createAdditionalOptica(nuevaSucursalNombre) { ok, msg ->
                            creatingSucursal = false
                            dialogMsg = msg
                            showDialog = true
                            if (ok) {
                                nuevaSucursalNombre = ""
                            }
                        }
                    }
                )
            }

            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.config_subscription_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    val planLabel = when (planCode) {
                        PlanCode.FREE -> "Free (máx. ${com.example.optoapp.subscription.SubscriptionManager.FREE_MAX_PACIENTES} pacientes)"
                        PlanCode.PRO_INDIVIDUAL -> "Pro Individual (1 óptica)"
                        PlanCode.PRO_MULTISITE_15 -> "Pro Multi-sede 15"
                        PlanCode.ENTERPRISE -> "Enterprise"
                        PlanCode.DEV_OWNER -> "Dev Owner (interno, ilimitado y exento de facturación)"
                    }
                    Text(stringResource(R.string.config_subscription_plan_label, planLabel), fontSize = 14.sp)
                    if (BuildConfig.DEBUG && BuildConfig.FORCE_PRO_DEV) {
                        Text(
                            stringResource(R.string.config_subscription_force_pro_debug),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        stringResource(R.string.config_subscription_play_product, PlayBillingManager.SUBSCRIPTION_PRODUCT_ID),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (planCode != PlanCode.DEV_OWNER) {
                        Button(
                            onClick = {
                                val act = context as? Activity
                                if (act != null) {
                                    subscriptionVm.launchProPurchase(act) { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.config_subscription_buy_action))
                        }
                    } else {
                        AssistChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.config_subscription_internal_billing_disabled)) }
                        )
                    }
                    if (BuildConfig.DEBUG) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.config_subscription_dev_mode_title), fontSize = 14.sp)
                                Text(stringResource(R.string.config_subscription_dev_mode_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(stringResource(R.string.config_subscription_sync_plan_action))
                    }
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.config_sync_diag_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.config_sync_diag_desc),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Auditoría de Tiempo para depuración
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            val systemZone = java.time.ZoneId.systemDefault().id
                            val effectiveZone = userTimeZone ?: systemZone
                            val localNow = java.time.ZonedDateTime.now(java.time.ZoneId.of(effectiveZone))
                            
                            Text("Reloj Efectivo: ${localNow.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Sistema: $systemZone | Manual: ${userTimeZone ?: "Ninguna"}", fontSize = 10.sp)
                            Text("Fecha Operativa: ${localNow.toLocalDate()}", fontSize = 10.sp)
                        }
                    }

                    HorizontalDivider()
                    Text(
                        "Estado remoto (servidor)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (remoteSyncTelemetryLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Text("Consultando última sincronización remota…", fontSize = 12.sp)
                        }
                    } else {
                        val remote = remoteSyncTelemetry
                        if (remote == null) {
                            Text(
                                "Sin registro remoto aún para esta óptica.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val statusColor = if (remote.lastStatus == "ok") {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                            Text(
                                "Estado: ${remote.lastStatus.uppercase()}",
                                fontSize = 12.sp,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Etapa: ${remote.lastStage.ifBlank { "n/a" }}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Última sync: ${formatRemoteSyncDateTime(remote.lastSyncAt, userTimeZone)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Actualizado ${formatRelativeSyncAge(remote.lastSyncAt)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (remote.lastError.isNotBlank()) {
                                Text(
                                    "Error: ${remote.lastError}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    remoteSyncTelemetryError?.let { err ->
                        Text(
                            "No se pudo leer telemetría remota: $err",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    OutlinedButton(
                        onClick = { syncDiagVm.refreshRemoteTelemetry() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Actualizar estado remoto")
                    }
                    HorizontalDivider()
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
                                stringResource(R.string.config_sync_diag_empty),
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
                                Toast.makeText(context, context.getString(R.string.config_sync_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.config_sync_copy_all, syncErrors.size))
                        }
                        TextButton(
                            onClick = {
                                syncDiagVm.clearErrorHistory()
                                Toast.makeText(context, context.getString(R.string.config_sync_cleared), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.config_sync_clear_list))
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
                    Text(stringResource(R.string.config_data_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.config_data_section_desc), fontSize = 14.sp)
                    if (!canManageBackups) {
                        Text(
                            stringResource(R.string.config_data_admin_only_backup),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Button(
                        onClick = { createBackupLauncher.launch("OptoApp_Backup_${System.currentTimeMillis()}.json") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canManageBackups
                    ) {
                        Text(stringResource(R.string.config_backup_download_action))
                    }
                    
                    HorizontalDivider()
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.config_data_restore_warning), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                    
                    OutlinedButton(
                        onClick = { restoreBackupLauncher.launch("application/json") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canManageBackups,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.config_backup_restore_action))
                    }
                }
            }
        }
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.config_dialog_ok)) } },
            title = { Text(stringResource(R.string.config_dialog_info_title)) },
            text = { Text(dialogMsg) }
        )
    }
}

@Composable
private fun SecuritySection(
    pinHasBeenSet: Boolean,
    isPinRequired: Boolean,
    pinActual: String,
    nuevoPin: String,
    confirmPin: String,
    onPinActualChange: (String) -> Unit,
    onNuevoPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onPinRequiredChange: (Boolean) -> Unit,
    onUpdatePin: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.config_security_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.config_security_pin_required_title), fontSize = 16.sp)
                    Text(
                        stringResource(R.string.config_security_pin_required_desc),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isPinRequired,
                    onCheckedChange = onPinRequiredChange
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (pinHasBeenSet) {
                OutlinedTextField(
                    value = pinActual,
                    onValueChange = { onPinActualChange(it.filter { c -> c.isDigit() }.take(SecurityManager.PIN_LENGTH)) },
                    label = { Text(stringResource(R.string.config_pin_current_label, SecurityManager.PIN_LENGTH)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            OutlinedTextField(
                value = nuevoPin,
                onValueChange = { onNuevoPinChange(it.filter { c -> c.isDigit() }.take(SecurityManager.PIN_LENGTH)) },
                label = { Text(stringResource(R.string.config_pin_new_label, SecurityManager.PIN_LENGTH)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { onConfirmPinChange(it.filter { c -> c.isDigit() }.take(SecurityManager.PIN_LENGTH)) },
                label = { Text(stringResource(R.string.config_pin_confirm_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(
                onClick = onUpdatePin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (pinHasBeenSet) stringResource(R.string.config_pin_update_action)
                    else "Crear PIN"
                )
            }
        }
    }
}

@Composable
private fun UsuariosRolesSection(
    roleUi: com.example.optoapp.viewmodel.RoleManagementUiState,
    allowedRoles: List<String>,
    canAssignAdminRole: Boolean,
    onEmailChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onAssignRole: () -> Unit,
    onRefresh: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.config_users_roles_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.config_users_roles_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OptoTextField(
                value = roleUi.emailInput,
                onValueChange = onEmailChange,
                label = stringResource(R.string.config_users_roles_email_label)
            )
            DropdownField(
                label = stringResource(R.string.config_users_roles_role_label),
                selected = roleUi.roleInput,
                options = allowedRoles,
                onSelected = onRoleChange
            )
            if (!canAssignAdminRole) {
                Text(
                    stringResource(R.string.config_users_roles_admin_only),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAssignRole,
                    enabled = !roleUi.loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.config_users_roles_assign_action)) }
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !roleUi.loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.config_users_roles_refresh_action)) }
            }
            roleUi.message?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp) }
            roleUi.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            if (roleUi.members.isEmpty()) {
                Text(stringResource(R.string.config_users_roles_empty), fontSize = 12.sp)
            } else {
                roleUi.members.take(20).forEach { row ->
                    Text("• ${row.email.ifBlank { row.userId }} — ${row.rol}", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SucursalesSection(
    nuevaSucursalNombre: String,
    creatingSucursal: Boolean,
    onNombreChange: (String) -> Unit,
    onCreate: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.config_branches_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.config_branches_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OptoTextField(
                value = nuevaSucursalNombre,
                onValueChange = onNombreChange,
                label = stringResource(R.string.config_branches_name_label)
            )
            Button(
                onClick = onCreate,
                enabled = nuevaSucursalNombre.trim().isNotEmpty() && !creatingSucursal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (creatingSucursal) {
                        stringResource(R.string.config_branches_creating_action)
                    } else {
                        stringResource(R.string.config_branches_create_action)
                    }
                )
            }
        }
    }
}

@Composable
private fun PlanManagementSection(
    planUi: PlanManagementUiState,
    canUseInternalPlan: Boolean,
    onPlanCodeChange: (String) -> Unit,
    onPlanStatusChange: (String) -> Unit,
    onMaxOpticasChange: (String) -> Unit,
    onMaxPacientesChange: (String) -> Unit,
    onMaxUsuariosChange: (String) -> Unit,
    onApplyPreset: () -> Unit,
    onSave: () -> Unit,
    onReload: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.config_plan_management_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.config_plan_management_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            DropdownField(
                label = stringResource(R.string.config_plan_label),
                selected = planUi.planCode,
                options = if (canUseInternalPlan) {
                    listOf("free", "pro_individual", "pro_multisite_15", "enterprise", "dev_owner")
                } else {
                    listOf("free", "pro_individual", "pro_multisite_15", "enterprise")
                },
                onSelected = onPlanCodeChange
            )
            DropdownField(
                label = stringResource(R.string.config_plan_status_label),
                selected = planUi.planStatus,
                options = listOf("active", "grace", "canceled"),
                onSelected = onPlanStatusChange
            )
            OptoTextField(
                value = planUi.maxOpticasInput,
                onValueChange = onMaxOpticasChange,
                label = stringResource(R.string.config_plan_max_opticas_label),
                keyboardType = KeyboardType.Number
            )
            OptoTextField(
                value = planUi.maxPacientesInput,
                onValueChange = onMaxPacientesChange,
                label = stringResource(R.string.config_plan_max_pacientes_label),
                keyboardType = KeyboardType.Number
            )
            OptoTextField(
                value = planUi.maxUsuariosInput,
                onValueChange = onMaxUsuariosChange,
                label = stringResource(R.string.config_plan_max_usuarios_label),
                keyboardType = KeyboardType.Number
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onApplyPreset,
                        enabled = !planUi.loading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.config_plan_apply_preset), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = onSave,
                        enabled = !planUi.loading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.config_plan_save_action), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                OutlinedButton(
                    onClick = onReload,
                    enabled = !planUi.loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.config_plan_reload))
                }
            }
            planUi.message?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp) }
            planUi.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun ClinicalIntegritySection(
    onResolveDuplicates: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.config_clinical_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.config_clinical_section_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onResolveDuplicates,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.config_clinical_resolve_ho_action))
            }
        }
    }
}

private data class FiscalDraftUpdate(
    val nombreComercial: String? = null,
    val docTipo: String? = null,
    val docNumero: String? = null,
    val razonSocial: String? = null,
    val direccionFiscal: String? = null,
    val distritoCiudadDepartamento: String? = null,
    val moneda: String? = null,
    val pais: String? = null,
    val contactoWhatsappTelefono: String? = null
)

@Composable
private fun FiscalDataSection(
    fiscalUi: com.example.optoapp.viewmodel.FiscalConfigUi,
    onDraftChange: (FiscalDraftUpdate) -> Unit,
    onSave: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.config_fiscal_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.config_fiscal_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OptoTextField(
                value = fiscalUi.nombreComercial,
                onValueChange = { onDraftChange(FiscalDraftUpdate(nombreComercial = it)) },
                label = stringResource(R.string.config_fiscal_nombre_comercial_label)
            )
            DropdownField(
                label = stringResource(R.string.config_fiscal_tipo_doc_label),
                selected = fiscalUi.docTipo,
                options = listOf("RUC", "RUS"),
                onSelected = { onDraftChange(FiscalDraftUpdate(docTipo = it)) }
            )
            OptoTextField(
                value = fiscalUi.docNumero,
                onValueChange = { onDraftChange(FiscalDraftUpdate(docNumero = it)) },
                label = stringResource(R.string.config_fiscal_numero_doc_label, fiscalUi.docTipo)
            )
            OptoTextField(
                value = fiscalUi.razonSocial,
                onValueChange = { onDraftChange(FiscalDraftUpdate(razonSocial = it)) },
                label = stringResource(R.string.config_fiscal_razon_social_label)
            )
            OptoTextField(
                value = fiscalUi.direccionFiscal,
                onValueChange = { onDraftChange(FiscalDraftUpdate(direccionFiscal = it)) },
                label = stringResource(R.string.config_fiscal_direccion_label)
            )
            OptoTextField(
                value = fiscalUi.distritoCiudadDepartamento,
                onValueChange = { onDraftChange(FiscalDraftUpdate(distritoCiudadDepartamento = it)) },
                label = stringResource(R.string.config_fiscal_distrito_label)
            )
            OptoTextField(
                value = fiscalUi.moneda,
                onValueChange = { onDraftChange(FiscalDraftUpdate(moneda = it)) },
                label = stringResource(R.string.config_fiscal_moneda_label)
            )
            OptoTextField(
                value = fiscalUi.pais,
                onValueChange = { onDraftChange(FiscalDraftUpdate(pais = it)) },
                label = stringResource(R.string.config_fiscal_pais_label)
            )
            OptoTextField(
                value = fiscalUi.contactoWhatsappTelefono,
                onValueChange = { onDraftChange(FiscalDraftUpdate(contactoWhatsappTelefono = it)) },
                label = stringResource(R.string.config_fiscal_contacto_label),
                keyboardType = KeyboardType.Phone
            )
            Button(
                onClick = onSave,
                enabled = !fiscalUi.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (fiscalUi.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.config_save_fiscal))
                }
            }
            fiscalUi.message?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp) }
            fiscalUi.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        }
    }
}

private fun formatRemoteSyncDateTime(raw: String?, overrideZoneId: String?): String {
    if (raw.isNullOrBlank()) return "No disponible"
    return runCatching {
        val utcDate = OffsetDateTime.parse(raw)
        val zoneId = if (!overrideZoneId.isNullOrBlank()) {
            ZoneId.of(overrideZoneId)
        } else {
            java.util.TimeZone.getDefault().toZoneId()
        }
        val localDate = utcDate.atZoneSameInstant(zoneId)
        localDate.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
    }.getOrDefault(raw ?: "")
}

private fun formatRelativeSyncAge(raw: String?): String {
    if (raw.isNullOrBlank()) return "hace un momento"
    return runCatching {
        val then = OffsetDateTime.parse(raw).toInstant()
        val now = java.time.Instant.now()
        val seconds = java.time.Duration.between(then, now).seconds.coerceAtLeast(0)
        when {
            seconds < 60 -> "hace menos de 1 min"
            seconds < 3600 -> "hace ${seconds / 60} min"
            seconds < 86400 -> "hace ${seconds / 3600} h"
            else -> "hace ${seconds / 86400} d"
        }
    }.getOrDefault("recientemente")
}
