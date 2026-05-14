package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.OpticaHeaderUi
import com.example.optoapp.viewmodel.SyncState
import com.example.optoapp.viewmodel.SyncViewModel
import com.example.optoapp.util.SyncErrorSanitizer

private fun NavController.navigateDrawer(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun DrawerContent(
    currentRoute: String?,
    drawerState: DrawerState,
    navController: NavController,
    opticaHeader: OpticaHeaderUi,
    showCierreCaja: Boolean,
    showBiYReportes: Boolean,
    showOperacionHoy: Boolean,
    syncState: SyncState,
    syncViewModel: SyncViewModel,
    authViewModel: AuthViewModel,
    parentNavController: NavController
) {
    val scope = rememberCoroutineScope()
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(syncState) {
        when (syncState) {
            is SyncState.Success -> {
                android.widget.Toast.makeText(
                    context,
                    (syncState as SyncState.Success).message,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                syncViewModel.clearSyncUiState()
            }
            is SyncState.Error -> {
                errorMessage = SyncErrorSanitizer.forUserMessage(
                    (syncState as SyncState.Error).message
                )
                showErrorDialog = true
            }
            else -> {}
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                syncViewModel.clearSyncUiState()
            },
            confirmButton = {
                TextButton(onClick = {
                    showErrorDialog = false
                    syncViewModel.clearSyncUiState()
                }) { Text("Entendido") }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Error de Sincronización")
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(errorMessage)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        // Header con logo y óptica
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "O",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "OptoApp",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = opticaHeader.nombreOptica.ifBlank { "Sin óptica" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Gestión
        Text(
            text = "GESTIÓN",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp)
        )
        NavigationDrawerItem(
            label = { Text("Pacientes", fontWeight = FontWeight.SemiBold) },
            selected = currentRoute == "pacientes",
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigateDrawer("pacientes")
            },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Servicios Varios", fontWeight = FontWeight.SemiBold) },
            selected = currentRoute == "servicios_extra",
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigateDrawer("servicios_extra")
            },
            icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        if (showOperacionHoy) {
            NavigationDrawerItem(
                label = { Text("Operación de Hoy", fontWeight = FontWeight.SemiBold) },
                selected = currentRoute == "operacion_hoy",
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigateDrawer("operacion_hoy")
                },
                icon = { Icon(Icons.Default.Today, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        // Programación
        Text(
            text = "PROGRAMACIÓN",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp)
        )
        NavigationDrawerItem(
            label = { Text("Agenda", fontWeight = FontWeight.SemiBold) },
            selected = currentRoute == "agenda",
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigateDrawer("agenda")
            },
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Inventario", fontWeight = FontWeight.SemiBold) },
            selected = currentRoute == "monturas",
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigateDrawer("monturas")
            },
            icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        // Finanzas
        if (showCierreCaja || showBiYReportes) {
            Text(
                text = "FINANZAS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp)
            )
            if (showCierreCaja) {
                NavigationDrawerItem(
                    label = { Text("Cierre de Caja", fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == "cierre_caja",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigateDrawer("cierre_caja")
                    },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
            if (showBiYReportes) {
                NavigationDrawerItem(
                    label = { Text("Estadísticas (BI)", fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == "estadisticas_bi",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigateDrawer("estadisticas_bi")
                    },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Reportes", fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == "reportes",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigateDrawer("reportes")
                    },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }

        // Sistema
        Text(
            text = "SISTEMA",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp)
        )
        NavigationDrawerItem(
            label = { Text("Configuración", fontWeight = FontWeight.SemiBold) },
            selected = currentRoute == "configuracion",
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigateDrawer("configuracion")
            },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        // Sync + Logout
        NavigationDrawerItem(
            label = {
                Text(
                    if (syncState is SyncState.Loading) "Sincronizando..." else "Sincronizar Cloud",
                    fontWeight = FontWeight.SemiBold
                )
            },
            selected = false,
            onClick = {
                if (syncState !is SyncState.Loading) {
                    syncViewModel.performFullSync()
                }
            },
            icon = {
                if (syncState is SyncState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.CloudSync, contentDescription = null)
                }
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
        NavigationDrawerItem(
            label = { Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error) },
            selected = false,
            onClick = {
                scope.launch {
                    drawerState.close()
                    authViewModel.logout()
                    parentNavController.navigate("login") {
                        popUpTo(parentNavController.graph.id) { inclusive = true }
                    }
                }
            },
            icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Salir") },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
