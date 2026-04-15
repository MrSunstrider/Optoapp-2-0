package com.example.optoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.optoapp.ui.screens.*
import com.example.optoapp.ui.theme.OptoAppTheme
import com.example.optoapp.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permiso de notificaciones en Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }

        setContent {
            OptoAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OptoAppNavigation()
                }
            }
        }
    }
}

@Composable
fun OptoAppNavigation() {
    val navController = rememberNavController()

    // AuthViewModel compartido
    val authViewModel: AuthViewModel = hiltViewModel()
    val isAuthChecked by authViewModel.isAuthChecked.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = null)
    val isPinRequired by authViewModel.isPinRequired.collectAsState(initial = null)

    // Guardia global: si la sesión se invalida, volver al login vaciando la pila
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == false) {
            navController.navigate("login") {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = "loading") {
        composable("loading") {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
            
            // Redirección inicial tras verificar sesión
            LaunchedEffect(isAuthChecked) {
                if (isAuthChecked) {
                    val dest = when {
                        isLoggedIn == false -> "login"
                        isPinRequired == true -> "pin"
                        else -> "main"
                    }
                    // Evitar navegar si ya estamos en el destino (prevención simple)
                    if (navController.currentDestination?.route != dest) {
                        navController.navigate(dest) {
                            popUpTo("loading") { inclusive = true }
                        }
                    }
                } else if (isAuthChecked == false) {
                    authViewModel.checkExistingSession()
                }
            }
        }

        composable("pin") { PinScreen(navController, viewModel = authViewModel) }
        composable("login") { LoginScreen(navController, viewModel = authViewModel) }
        composable("seleccion_optica") { SeleccionOpticaScreen(navController, viewModel = authViewModel) }
        composable("main") { MainDrawerScreen(navController, authViewModel = authViewModel) }
    }
}

