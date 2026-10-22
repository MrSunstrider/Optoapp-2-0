package com.example.optoapp.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import com.example.optoapp.R
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.optoapp.data.AppRoles
import com.example.optoapp.ui.components.OfflineBanner
import com.example.optoapp.ui.navigation.CostosTab
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.ui.screens.costos.OtsSinCostoScreen
import com.example.optoapp.ui.screens.ordenescompra.OrdenesCompraScreen
import com.example.optoapp.domain.AppThemeMode
import com.example.optoapp.util.MesArg
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.OpticaHeaderViewModel
import com.example.optoapp.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

// Screens show snackbars without receiving the host through every composable signature.
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

@Composable
fun MainDrawerScreen(
    parentNavController: NavController,
    /** Misma instancia que [LoginScreen] / [PinScreen] en [MainActivity]; si no, logout no resetea el estado que lee el login. */
    authViewModel: AuthViewModel,
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val syncViewModel: com.example.optoapp.viewmodel.SyncViewModel = hiltViewModel()
    val opticaHeaderViewModel: OpticaHeaderViewModel = hiltViewModel()
    // WHY: same Activity store as MainActivity so the banner toggle drives OptoAppTheme immediately
    val themeViewModel: ThemeViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
    val syncState by syncViewModel.syncState.collectAsState()
    val isSilentSyncing by syncViewModel.isSilentSyncing.collectAsState()
    val opticaHeader by opticaHeaderViewModel.uiState.collectAsState()
    val themeMode by themeViewModel.mode.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val effectiveDark = AppThemeMode.resolveDark(themeMode, systemDark)
    val opticaRol by authViewModel.opticaRol.collectAsState(initial = "")
    val showCierreCaja = AppRoles.canViewCierreCaja(opticaRol)
    val showBiYReportes = AppRoles.canViewBiAndReports(opticaRol)
    val showOperacionHoy = AppRoles.canViewOperacionHoy(opticaRol)
    val showConfiguracion = AppRoles.canManageUsers(opticaRol)

    // Refreshes on entry because other devices may have changed the shared data while this one was away.
    LaunchedEffect(Unit) {
        syncViewModel.performSilentSync()
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val isOnline by syncViewModel.isOnline.collectAsState()
    val snackbarScope = rememberCoroutineScope()

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                    DrawerContent(
                        currentRoute = currentRoute,
                        drawerState = drawerState,
                        navController = navController,
                        opticaHeader = opticaHeader,
                        showCierreCaja = showCierreCaja,
                        showBiYReportes = showBiYReportes,
                        showOperacionHoy = showOperacionHoy,
                        showConfiguracion = showConfiguracion,
                        syncState = syncState,
                        syncViewModel = syncViewModel,
                        authViewModel = authViewModel,
                        parentNavController = parentNavController,
                    )
                }
            },
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isSilentSyncing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                    Surface(
                        tonalElevation = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        when (val prep = authViewModel.prepareOpticaSelection()) {
                                            is com.example.optoapp.viewmodel.OpticaSelectionPrep.Error -> {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    prep.message.ifBlank { context.getString(R.string.drawer_error_cargar_opticas) },
                                                    android.widget.Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                            is com.example.optoapp.viewmodel.OpticaSelectionPrep.Ok -> {
                                                if (prep.hasMultiple) {
                                                    parentNavController.navigate(Route.SeleccionOptica.route)
                                                } else {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        context.getString(R.string.drawer_una_optica),
                                                        android.widget.Toast.LENGTH_SHORT,
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.drawer_optica_activa,
                                        opticaHeader.nombreOptica,
                                        opticaHeader.fiscalEtiqueta,
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(
                                onClick = { themeViewModel.toggle(systemDark) },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    imageVector = if (effectiveDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = stringResource(
                                        if (effectiveDark) R.string.theme_toggle_to_light else R.string.theme_toggle_to_dark,
                                    ),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    OfflineBanner(isOnline = isOnline)

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(navController = navController, startDestination = Route.OperacionHoy.route, modifier = Modifier.fillMaxSize()) {
                            composable(Route.Pacientes.route) { PacientesListScreen(navController, drawerState) }
                            composable(Route.Agenda.route) { AgendaScreen(navController, drawerState) }
                            composable(Route.NuevoPaciente.route) { NuevoPacienteScreen(navController) }
                            composable(Route.EditarPaciente("{id}").route) { backStackEntry ->
                                NuevoPacienteScreen(navController, pacienteId = backStackEntry.arguments?.getString("id"))
                            }
                            composable(Route.DetallePaciente("{id}").route) { backStackEntry ->
                                val pid = backStackEntry.arguments?.getString("id")
                                if (pid.isNullOrBlank()) {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                    Box(Modifier.fillMaxSize())
                                } else {
                                    DetallePacienteScreen(navController, id = pid)
                                }
                            }
                            composable(Route.NuevaEvaluacion("{pacienteId}").route) { backStackEntry ->
                                val pacienteId = backStackEntry.arguments?.getString("pacienteId")
                                if (pacienteId.isNullOrBlank()) {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                    Box(Modifier.fillMaxSize())
                                } else {
                                    NuevaEvaluacionScreen(navController, pacienteId = pacienteId)
                                }
                            }
                            composable(Route.EditarEvaluacion("{pacienteId}", "{evalId}").route) { backStackEntry ->
                                val pacienteId = backStackEntry.arguments?.getString("pacienteId")
                                if (pacienteId.isNullOrBlank()) {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                    Box(Modifier.fillMaxSize())
                                } else {
                                    NuevaEvaluacionScreen(
                                        navController,
                                        pacienteId = pacienteId,
                                        evaluacionId = backStackEntry.arguments?.getString("evalId"),
                                    )
                                }
                            }
                            composable(Route.NuevaDispensacion("{pacienteId}").route) { backStackEntry ->
                                val pacienteId = backStackEntry.arguments?.getString("pacienteId")
                                if (pacienteId.isNullOrBlank()) {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                    Box(Modifier.fillMaxSize())
                                } else {
                                    NuevaDispensacionScreen(navController, pacienteId = pacienteId)
                                }
                            }
                            composable(
                                Route.EditarDispensacion.PATTERN,
                                arguments = listOf(
                                    navArgument(Route.EditarDispensacion.SECCION_ARG) {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                ),
                            ) { backStackEntry ->
                                val pacienteId = backStackEntry.arguments?.getString("pacienteId")
                                if (pacienteId.isNullOrBlank()) {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                    Box(Modifier.fillMaxSize())
                                } else {
                                    NuevaDispensacionScreen(
                                        navController,
                                        pacienteId = pacienteId,
                                        dispensacionId = backStackEntry.arguments?.getString("dispId"),
                                        seccion = backStackEntry.arguments?.getString(Route.EditarDispensacion.SECCION_ARG),
                                    )
                                }
                            }
                            composable(Route.ServiciosExtra.route) {
                                ServiciosExtraScreen(navController, drawerState)
                            }
                            composable(Route.Monturas.route) { MonturasScreen(navController) }
                            composable(Route.Proveedores.route) { ProveedoresScreen(navController) }
                            composable(Route.OrdenesCompra.route) { OrdenesCompraScreen(navController) }
                            composable(Route.InventarioFisico.route) { com.example.optoapp.ui.screens.inventariofisico.InventarioFisicoScreen(navController) }
                            composable(Route.OperacionHoy.route) { OperacionHoyScreen(navController, drawerState) }
                            composable(Route.NuevoServicio.route) {
                                NuevoServicioScreen(navController, pacienteId = null)
                            }
                            composable(Route.NuevoServicioPaciente("{pacienteId}").route) { backStackEntry ->
                                NuevoServicioScreen(navController, pacienteId = backStackEntry.arguments?.getString("pacienteId"))
                            }
                            composable(Route.EditarServicio("{id}").route) { backStackEntry ->
                                NuevoServicioScreen(navController, servicioId = backStackEntry.arguments?.getString("id"))
                            }
                            composable(Route.Reportes.route) { ReportesScreen(drawerState) }
                            composable(
                                Route.CostosYGastos.route,
                                arguments = listOf(
                                    navArgument(CostosTab.KEY) {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                    navArgument(MesArg.KEY) {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                ),
                            ) { CostosYGastosScreen(navController, drawerState) }
                            composable(Route.CierreCaja.route) { CierreCajaScreen(navController) }
                            composable(Route.EstadisticasBI.route) { AnalisisNegocioScreen(navController) }
                            composable(
                                Route.AnalisisDetalle.route,
                                arguments = listOf(
                                    navArgument(MesArg.KEY) {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                ),
                            ) { AnalisisDetalleScreen(navController) }
                            composable(
                                Route.OtsSinCosto.route,
                                arguments = listOf(
                                    navArgument(MesArg.KEY) {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                ),
                            ) { OtsSinCostoScreen(navController) }
                            composable(Route.Configuracion.route) {
                                if (!showConfiguracion) {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                    Box(Modifier.fillMaxSize())
                                } else {
                                    ConfiguracionScreen(navController, drawerState, syncViewModel)
                                }
                            }
                            composable(Route.Conflictos.route) { ConflictosScreen(navController, syncViewModel) }
                            composable(Route.InformacionFinanciera("{dispensacionId}").route) { backStackEntry ->
                                val dispId = backStackEntry.arguments?.getString("dispensacionId")
                                if (dispId.isNullOrBlank()) {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                    Box(Modifier.fillMaxSize())
                                } else {
                                    InformacionFinancieraScreen(navController, dispensacionId = dispId)
                                }
                            }
                        }
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}
