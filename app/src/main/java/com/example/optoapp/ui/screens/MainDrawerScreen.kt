package com.example.optoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

@Composable
fun MainDrawerScreen(parentNavController: NavController) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Avatar",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "OptoApp", style = MaterialTheme.typography.titleLarge)
                    Text(text = "Usuario Único", style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Pacientes") },
                    selected = currentRoute == "pacientes",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("pacientes") {
                            popUpTo("pacientes") { inclusive = true }
                        }
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Servicios Varios") },
                    selected = currentRoute == "servicios_extra",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("servicios_extra")
                    },
                    icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Cierre de Caja") },
                    selected = currentRoute == "cierre_caja",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("cierre_caja")
                    },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Estadísticas (BI)") },
                    selected = currentRoute == "estadisticas_bi",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("estadisticas_bi")
                    },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Reportes") },
                    selected = currentRoute == "reportes",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("reportes")
                    },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Configuración") },
                    selected = currentRoute == "configuracion",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("configuracion")
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.weight(1f))
                NavigationDrawerItem(
                    label = { Text("Cerrar Sesión") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            parentNavController.navigate("pin") {
                                popUpTo("main") { inclusive = true }
                            }
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Salir") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        NavHost(navController = navController, startDestination = "pacientes") {
            composable("pacientes") { PacientesListScreen(navController, drawerState) }
            composable("nuevoPaciente") { NuevoPacienteScreen(navController) }
            composable("editarPaciente/{id}") { backStackEntry ->
                NuevoPacienteScreen(navController, pacienteId = backStackEntry.arguments?.getString("id"))
            }
            composable("detallePaciente/{id}") { backStackEntry ->
                DetallePacienteScreen(navController, id = backStackEntry.arguments?.getString("id")!!)
            }
            composable("nuevaEvaluacion/{pacienteId}") { backStackEntry ->
                NuevaEvaluacionScreen(navController, pacienteId = backStackEntry.arguments?.getString("pacienteId")!!)
            }
            composable("editarEvaluacion/{pacienteId}/{evalId}") { backStackEntry ->
                NuevaEvaluacionScreen(
                    navController, 
                    pacienteId = backStackEntry.arguments?.getString("pacienteId")!!,
                    evaluacionId = backStackEntry.arguments?.getString("evalId")
                )
            }
            composable("nuevaDispensacion/{pacienteId}") { backStackEntry ->
                NuevaDispensacionScreen(navController, pacienteId = backStackEntry.arguments?.getString("pacienteId")!!)
            }
            composable("editarDispensacion/{pacienteId}/{dispId}") { backStackEntry ->
                NuevaDispensacionScreen(
                    navController, 
                    pacienteId = backStackEntry.arguments?.getString("pacienteId")!!,
                    dispensacionId = backStackEntry.arguments?.getString("dispId")
                )
            }
            composable("servicios_extra") { 
                ServiciosExtraScreen(navController, drawerState) 
            }
            composable("nuevo_servicio/{pacienteId}") { backStackEntry ->
                NuevoServicioScreen(navController, pacienteId = backStackEntry.arguments?.getString("pacienteId"))
            }
            composable("editar_servicio/{id}") { backStackEntry ->
                NuevoServicioScreen(navController, servicioId = backStackEntry.arguments?.getString("id"))
            }
            composable("reportes") { ReportesScreen(drawerState) }
            composable("cierre_caja") { CierreCajaScreen(navController) }
            composable("estadisticas_bi") { BIScreen(navController) }
            composable("configuracion") { ConfiguracionScreen(navController, drawerState) }
        }
    }
}
