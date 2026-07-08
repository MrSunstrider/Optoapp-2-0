package com.example.optoapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.optoapp.BuildConfig
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
    showConfiguracion: Boolean,
    syncState: SyncState,
    syncViewModel: SyncViewModel,
    authViewModel: AuthViewModel,
    parentNavController: NavController
) {
    val scope = rememberCoroutineScope()
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val conflictCount by syncViewModel.conflictCount.collectAsState()

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
                    Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(com.example.optoapp.R.drawable.logo_login),
                contentDescription = "OptoApp",
                modifier = Modifier.fillMaxWidth(0.65f).clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            )
            Text(
                text = opticaHeader.nombreOptica.ifBlank { "Sin óptica" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

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
            icon = { Icon(Icons.Default.Person, contentDescription = "Pacientes") },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Servicios Extra", fontWeight = FontWeight.SemiBold) },
            selected = currentRoute == "servicios_extra",
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigateDrawer("servicios_extra")
            },
            icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = "Servicios Extra") },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        if (showOperacionHoy) {
            NavigationDrawerItem(
                label = { Text("Dashboard", fontWeight = FontWeight.SemiBold) },
                selected = currentRoute == "operacion_hoy",
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigateDrawer("operacion_hoy")
                },
                icon = { Icon(Icons.Default.Today, contentDescription = "Dashboard") },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

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
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Agenda") },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Inventario", fontWeight = FontWeight.SemiBold) },
            selected = currentRoute == "monturas",
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigateDrawer("monturas")
            },
            icon = { Icon(Icons.Default.Inventory2, contentDescription = "Inventario") },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Proveedores", fontWeight = FontWeight.SemiBold) },
            selected = currentRoute == "proveedores",
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigateDrawer("proveedores")
            },
            icon = { Icon(Icons.Default.Business, contentDescription = "Proveedores") },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
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
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Cierre de Caja") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
            if (showBiYReportes) {
                NavigationDrawerItem(
                    label = { Text("Mi Negocio", fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == "estadisticas_bi",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigateDrawer("estadisticas_bi")
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Mi Negocio") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Reportes", fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == "reportes",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigateDrawer("reportes")
                    },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Reportes") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }

        // Sync + Conflictos — refrescar al abrir
        LaunchedEffect(Unit) { syncViewModel.refreshConflicts() }

        Text(
            text = "SISTEMA",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp)
        )
        if (showConfiguracion) {
            NavigationDrawerItem(
                label = { Text("Configuración", fontWeight = FontWeight.SemiBold) },
                selected = currentRoute == "configuracion",
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigateDrawer("configuracion")
                },
                icon = { Icon(Icons.Default.Settings, contentDescription = "Configuración") },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        if (conflictCount > 0) {
            NavigationDrawerItem(
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Conflictos", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text(
                                conflictCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                },
                selected = currentRoute == "conflictos",
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigateDrawer("conflictos")
                },
                icon = {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Conflictos",
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

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
                    Icon(Icons.Default.CloudSync, contentDescription = "Sincronizar")
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
