package com.example.optoapp.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.R
import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.SecurityManager
import com.example.optoapp.ui.components.config.ClinicalIntegritySection
import com.example.optoapp.ui.components.config.FiscalDraftUpdate
import com.example.optoapp.ui.components.config.DataManagementCard
import com.example.optoapp.ui.components.config.FiscalDataSection
import com.example.optoapp.ui.components.config.LaboratorySection
import com.example.optoapp.ui.components.config.PlanManagementSection
import com.example.optoapp.ui.components.config.SecuritySection
import com.example.optoapp.ui.components.config.SubscriptionCard
import com.example.optoapp.ui.components.config.SucursalesSection
import com.example.optoapp.ui.components.config.SyncDiagnosticsCard
import com.example.optoapp.ui.components.config.SystemSection
import com.example.optoapp.ui.components.config.UsuariosRolesSection
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.FiscalConfigViewModel
import com.example.optoapp.viewmodel.LaboratorioConfigViewModel
import com.example.optoapp.viewmodel.PlanManagementViewModel
import com.example.optoapp.viewmodel.RoleManagementViewModel
import com.example.optoapp.viewmodel.SettingsViewModel
import com.example.optoapp.viewmodel.SubscriptionViewModel
import com.example.optoapp.viewmodel.SyncDiagnosticsViewModel
import com.example.optoapp.viewmodel.SyncState
import com.example.optoapp.viewmodel.SyncViewModel
import kotlinx.coroutines.launch

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

    val pinHasBeenSet by settingsVm.pinHasBeenSet.collectAsState(initial = true)
    val isPinRequired by settingsVm.isPinRequired.collectAsState(initial = true)
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
            SectionHeader("SEGURIDAD Y ACCESO")
            SecuritySection(
                pinHasBeenSet = pinHasBeenSet,
                isPinRequired = isPinRequired,
                pinActual = pinActual,
                nuevoPin = nuevoPin,
                confirmPin = confirmPin,
                onPinActualChange = { pinActual = it },
                onNuevoPinChange = { nuevoPin = it },
                onConfirmPinChange = { confirmPin = it },
                onPinRequiredChange = { settingsVm.togglePinRequired(it) },
                onUpdatePin = {
                    if (nuevoPin == confirmPin && SecurityManager.isValidPin(nuevoPin)) {
                        scope.launch {
                            settingsVm.updatePin(pinActual, nuevoPin)
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

            // ─── SISTEMA ────────────────────────────────────────────────────
            SectionHeader("SISTEMA")
            SystemSection(
                userTimeZone = userTimeZone,
                availableTimeZones = availableTimeZones,
                remindersEnabled = remindersEnabled,
                notificationPermissionGranted = notificationPermissionGranted,
                systemNotificationsEnabled = systemNotificationsEnabled,
                onUserTimeZoneSelected = { selected ->
                    if (selected == "Detectar automáticamente") settingsVm.setUserTimeZone(null)
                    else settingsVm.setUserTimeZone(selected)
                },
                onRemindersEnabledChanged = settingsVm::setRemindersEnabled,
                onSendTestNotification = {
                    settingsVm.sendTestNotification()
                    Toast.makeText(context, context.getString(R.string.config_notification_test_sent), Toast.LENGTH_LONG).show()
                }
            )

            // ─── DATOS DE LA ÓPTICA ──────────────────────────────────────────
            SectionHeader("DATOS DE LA ÓPTICA")
            LaboratorySection(
                labNombre = labNombre,
                labContacto = labContacto,
                onLabNombreChange = { labNombre = it },
                onLabContactoChange = { labContacto = it },
                onSave = { laboratorioVm.save(labNombre, labContacto) }
            )

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

            // ─── SUSCRIPCIÓN ────────────────────────────────────────────────
            SubscriptionCard(
                planCode = planCode,
                devProOverride = devProOverride,
                subscriptionVm = subscriptionVm,
                context = context
            )
            // ─── SINCRONIZACIÓN ─────────────────────────────────────────────
            SyncDiagnosticsCard(
                syncErrors = syncErrors,
                remoteSyncTelemetry = remoteSyncTelemetry,
                remoteSyncTelemetryLoading = remoteSyncTelemetryLoading,
                remoteSyncTelemetryError = remoteSyncTelemetryError,
                userTimeZone = userTimeZone,
                syncDiagVm = syncDiagVm,
                context = context
            )
            // ─── GESTIÓN DE DATOS ───────────────────────────────────────────
            DataManagementCard(
                canManageBackups = canManageBackups,
                createBackupLauncher = createBackupLauncher,
                restoreBackupLauncher = restoreBackupLauncher
            )
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
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 2.dp, top = 8.dp)
    )
}
