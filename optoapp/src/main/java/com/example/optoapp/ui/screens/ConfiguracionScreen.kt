package com.example.optoapp.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.optoapp.R
import com.example.optoapp.data.AppRoles
import com.example.optoapp.ui.components.config.ClinicalIntegritySection
import com.example.optoapp.ui.components.config.ConfigAboutSection
import com.example.optoapp.ui.components.config.DataManagementCard
import com.example.optoapp.ui.components.config.FiscalDataSection
import com.example.optoapp.ui.components.config.LaboratorySection
import com.example.optoapp.ui.components.config.ConfigProfileSection
import com.example.optoapp.ui.components.config.SecuritySection
import com.example.optoapp.ui.components.config.SectionHeader
import com.example.optoapp.ui.components.config.SubscriptionCard
import com.example.optoapp.ui.components.config.SyncDiagnosticsCard
import com.example.optoapp.ui.components.config.SystemSection
import com.example.optoapp.ui.components.config.UsuariosRolesSection
import com.example.optoapp.ui.components.OptoTopAppBar
import com.example.optoapp.ui.theme.OptoTokens
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.ConfiguracionViewModel
import com.example.optoapp.viewmodel.FiscalConfigViewModel
import com.example.optoapp.viewmodel.LaboratorioConfigViewModel
import com.example.optoapp.viewmodel.RoleManagementViewModel
import com.example.optoapp.viewmodel.SettingsViewModel
import com.example.optoapp.viewmodel.SubscriptionViewModel
import com.example.optoapp.viewmodel.SyncDiagnosticsViewModel
import com.example.optoapp.viewmodel.SyncState
import com.example.optoapp.viewmodel.SyncViewModel
import kotlinx.coroutines.launch

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
    configVm: ConfiguracionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fiscalUi by fiscalVm.uiState.collectAsState()
    val labUi by laboratorioVm.uiState.collectAsState()
    val planCode by subscriptionVm.planCode.collectAsState()
    val devProOverride by subscriptionVm.devProOverride.collectAsState()
    val globalSyncState by syncVm.syncState.collectAsState()
    val userTimeZone by settingsVm.userTimeZone.collectAsState()
    val roleUi by roleVm.uiState.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState(initial = "")
    val opticaRol by viewModel.opticaRol.collectAsState(initial = "admin")
    val pinHasBeenSet by settingsVm.pinHasBeenSet.collectAsState(initial = true)
    val isPinRequired by settingsVm.isPinRequired.collectAsState(initial = true)
    val remindersEnabled by settingsVm.remindersEnabled.collectAsState()

    val canManageUsers = AppRoles.canManageUsers(opticaRol)
    val canManageBackups = AppRoles.canManageBackups(opticaRol)
    val canAssignAdminRole = AppRoles.canAssignAdminRole(opticaRol)
    val allowedRoles = remember(canAssignAdminRole) {
        if (canAssignAdminRole) listOf("especialista", "asesor", "asesora", "ventas", "invitado", "gerente", "admin")
        else listOf("especialista", "asesor", "asesora", "ventas", "invitado", "gerente")
    }

    val notificationPermissionGranted =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val systemNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

    LaunchedEffect(labUi.opticaId, labUi.laboratorioNombre, labUi.laboratorioContacto) {
        configVm.initLabFields(labUi.laboratorioNombre, labUi.laboratorioContacto)
    }
    LaunchedEffect(Unit) {
        fiscalVm.syncFromServer(); laboratorioVm.syncFromServer(); subscriptionVm.refreshPlanFromServer()
    }
    LaunchedEffect(fiscalUi.message, fiscalUi.error) {
        val msg = fiscalUi.message ?: fiscalUi.error
        if (msg != null) { configVm.dialogMessage = msg; fiscalVm.clearMessages() }
    }
    LaunchedEffect(canManageUsers) { if (canManageUsers) roleVm.loadMembers() }
    LaunchedEffect(globalSyncState) {
        if (globalSyncState is SyncState.Success || globalSyncState is SyncState.Error) syncDiagVm.refreshRemoteTelemetry()
    }
    LaunchedEffect(allowedRoles, roleUi.roleInput) {
        if (roleUi.roleInput !in allowedRoles) roleVm.updateRole(allowedRoles.first())
    }

    val createBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { scope.launch {
            runCatching {
                val json = viewModel.getBackupJson()
                context.contentResolver.openOutputStream(it)?.use { stream -> stream.write(json.toByteArray()) }
            }.onSuccess { configVm.dialogMessage = context.getString(R.string.config_backup_export_success) }
                .onFailure { e -> configVm.dialogMessage = e.message ?: context.getString(R.string.config_backup_export_failed) }
        } }
    }
    val restoreBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { scope.launch {
            context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }?.let { json ->
                viewModel.restoreBackup(json) { msg -> configVm.dialogMessage = msg }
            }
        } }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surface, contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
        OptoTopAppBar(
            title = stringResource(R.string.config_title),
            navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, contentDescription = "Menu") } }
        )
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = OptoTokens.spacing.lg, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(OptoTokens.spacing.md)) {

            SectionHeader(stringResource(R.string.config_section_security), icon = Icons.Default.Lock)
            SecuritySection(pinHasBeenSet = pinHasBeenSet, isPinRequired = isPinRequired,
                pinActual = configVm.pinActual, nuevoPin = configVm.nuevoPin, confirmPin = configVm.confirmPin,
                onPinActualChange = { configVm.pinActual = it }, onNuevoPinChange = { configVm.nuevoPin = it },
                onConfirmPinChange = { configVm.confirmPin = it },
                onPinRequiredChange = { settingsVm.togglePinRequired(it) }, onUpdatePin = { configVm.updatePin() })

            ConfigProfileSection(email = userEmail, rol = opticaRol, opticaName = fiscalUi.nombreComercial)

            SectionHeader(stringResource(R.string.config_section_system), icon = Icons.Default.Settings)
            SystemSection(userTimeZone = userTimeZone, availableTimeZones = configVm.availableTimeZones,
                remindersEnabled = remindersEnabled, notificationPermissionGranted = notificationPermissionGranted,
                systemNotificationsEnabled = systemNotificationsEnabled,
                onUserTimeZoneSelected = { selected -> if (selected == "Detectar automáticamente") settingsVm.setUserTimeZone(null) else settingsVm.setUserTimeZone(selected) },
                onRemindersEnabledChanged = settingsVm::setRemindersEnabled,
                onSendTestNotification = { settingsVm.sendTestNotification(); Toast.makeText(context, context.getString(R.string.config_notification_test_sent), Toast.LENGTH_LONG).show() })

            SectionHeader(stringResource(R.string.config_section_optica_data))
            LaboratorySection(labNombre = configVm.labNombre, labContacto = configVm.labContacto,
                onLabNombreChange = { configVm.labNombre = it }, onLabContactoChange = { configVm.labContacto = it },
                onSave = { laboratorioVm.save(configVm.labNombre, configVm.labContacto) })

            if (canManageUsers) {
                FiscalDataSection(fiscalUi = fiscalUi,
                    onDraftChange = { update -> fiscalVm.updateDraft(nombreComercial = update.nombreComercial, docTipo = update.docTipo, docNumero = update.docNumero, razonSocial = update.razonSocial, direccionFiscal = update.direccionFiscal); fiscalVm.clearMessages() },
                    onSave = fiscalVm::save)
            }
            if (canManageUsers) {
                ClinicalIntegritySection(onResolveDuplicates = { viewModel.resolveDuplicateHistorias { msg -> configVm.dialogMessage = msg } })
                UsuariosRolesSection(roleUi = roleUi, allowedRoles = allowedRoles, canAssignAdminRole = canAssignAdminRole,
                    onEmailChange = roleVm::updateEmail, onRoleChange = roleVm::updateRole,
                    onAssignRole = { roleVm.assignRole() }, onRefresh = { roleVm.loadMembers() })
            }
            SubscriptionCard(planCode = planCode, devProOverride = devProOverride, subscriptionVm = subscriptionVm, context = context)
            SyncDiagnosticsCard(syncDiagVm = syncDiagVm, context = context)
            DataManagementCard(canManageBackups = canManageBackups, createBackupLauncher = createBackupLauncher, restoreBackupLauncher = restoreBackupLauncher)

            ConfigAboutSection()
        }
    }

    configVm.dialogMessage?.let { msg ->
        AlertDialog(onDismissRequest = { configVm.dismissDialog() },
            confirmButton = { TextButton(onClick = { configVm.dismissDialog() }) { Text(stringResource(R.string.config_dialog_ok)) } },
            title = { Text(stringResource(R.string.config_dialog_info_title)) }, text = { Text(msg) })
    }
}
