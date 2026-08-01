package com.example.optoapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.optoapp.domain.observer.MembershipObserver
import com.example.optoapp.ui.components.OfflineBanner
import com.example.optoapp.util.NetworkMonitor
import com.example.optoapp.ui.components.UpdateDialog
import com.example.optoapp.ui.screens.*
import com.example.optoapp.ui.theme.OptoAppTheme
import com.example.optoapp.util.UpdateChecker
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.RecoveryState
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    @Inject lateinit var supabaseClient: SupabaseClient

    @Inject lateinit var supabaseObserver: com.example.optoapp.domain.observer.MembershipObserver

    @Inject lateinit var networkMonitor: NetworkMonitor

    private fun isRecoveryDeepLink(intent: Intent?): Boolean {
        val fragment = intent?.data?.fragment ?: return false
        return fragment.contains("type=recovery")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }

        if (intent?.action == Intent.ACTION_VIEW) {
            if (isRecoveryDeepLink(intent)) {
                authViewModel.handleRecoveryDeepLink(intent)
            } else {
                authViewModel.handleAuthDeepLinkIntent(intent)
            }
        }

        // P0-T4: validar sesión Supabase al arranque vs. confiar ciegamente en DataStore
        authViewModel.checkExistingSession()

        lifecycleScope.launch {
            authViewModel.userTimeZone.collect { tz ->
                com.example.optoapp.util.DateUtils.userPreferredZone = tz?.let {
                    try {
                        java.time.ZoneId.of(it)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }

        setContent {
            OptoAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    OptoAppNavigation(authViewModel, supabaseClient, supabaseObserver, networkMonitor)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isRecoveryDeepLink(intent)) {
            authViewModel.handleRecoveryDeepLink(intent)
        } else {
            authViewModel.handleAuthDeepLinkIntent(intent)
        }
    }
}

@Composable
fun OptoAppNavigation(
    authViewModel: AuthViewModel,
    supabaseClient: SupabaseClient,
    supabaseObserver: com.example.optoapp.domain.observer.MembershipObserver,
    networkMonitor: NetworkMonitor,
) {
    val navController = rememberNavController()

    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = null)
    val pinHasBeenSet by authViewModel.pinHasBeenSet.collectAsState(initial = null)
    val isPinRequired by authViewModel.isPinRequired.collectAsState(initial = null)
    val recoveryState by authViewModel.recoveryState.collectAsState()

    var updateInfo by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == true) {
            val info = withContext(kotlinx.coroutines.Dispatchers.IO) {
                UpdateChecker.check(supabaseClient)
            }
            updateInfo = info
        }
    }

    // Guardia global: si la sesión se invalida, volver al login vaciando la pila
    // No interfiere con el flujo de recuperación de contraseña
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == false &&
            recoveryState !is RecoveryState.LinkReceived &&
            recoveryState !is RecoveryState.PasswordUpdated
        ) {
            navController.navigate("login") {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    LaunchedEffect(recoveryState) {
        when (recoveryState) {
            is RecoveryState.LinkReceived -> {
                navController.navigate("new_password")
            }
            else -> {} // PasswordUpdated lo maneja NewPasswordScreen (mensaje + botón)
        }
    }

    Column {
        val isOnline by networkMonitor.isOnline.collectAsState(initial = true)
        OfflineBanner(isOnline = isOnline)
        NavHost(navController = navController, startDestination = "login", modifier = Modifier.weight(1f)) {
            composable("create_pin") { CreatePinScreen(navController, viewModel = authViewModel) }
            composable("pin") { PinScreen(navController, viewModel = authViewModel) }
            composable("login") { LoginScreen(navController, viewModel = authViewModel) }
            composable("register") { RegisterScreen(navController, viewModel = authViewModel) }
            composable("sin_optica") { SinOpticaScreen(navController, supabaseObserver, authViewModel) }
            composable("onboarding_optica") {
                @Suppress("DEPRECATION")
                OnboardingOpticaScreen(navController, viewModel = authViewModel)
            }
            composable("seleccion_optica") { SeleccionOpticaScreen(navController, viewModel = authViewModel) }
            composable("main") { MainDrawerScreen(navController, authViewModel = authViewModel) }
            composable("recovery") {
                RecoveryScreen(navController = navController, viewModel = authViewModel)
            }
            composable("new_password") {
                NewPasswordScreen(navController = navController, viewModel = authViewModel)
            }
        }
    }

    // UpdateCheck sobrevive a la navegación — se muestra sobre la pantalla activa
    updateInfo?.let { info ->
        UpdateDialog(updateInfo = info, onDismiss = { updateInfo = null })
    }
}
