package com.example.optoapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.ui.components.virtualtryon.*
import com.example.optoapp.viewmodel.TryOnState
import com.example.optoapp.viewmodel.VirtualTryOnViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Host screen for the Virtual Try-On feature.
 *
 * Manages photo selection, face detection, montura overlay,
 * measurement display, and result save/share.
 *
 * @param pacienteId Patient whose PD data will be used
 * @param onNavigateBack Callback to navigate back
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualTryOnScreen(
    pacienteId: String,
    onNavigateBack: () -> Unit
) {
    val viewModel: VirtualTryOnViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val selectedMontura by viewModel.selectedMontura.collectAsState()
    val monturas by viewModel.monturas.collectAsState()
    val monturasLoading by viewModel.monturasLoading.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Bottom sheet visibility
    var showMonturaSelector by remember { mutableStateOf(false) }

    // ─── Permissions ───────────────────────────────────────────────────
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        // permission result used automatically by the system
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // ─── Gallery picker (GetContent) ───────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectPhoto(it) }
    }

    // ─── Load PD on mount ──────────────────────────────────────────────
    LaunchedEffect(pacienteId) {
        viewModel.loadPatientPD(pacienteId)
    }

    // ─── Load monturas when bottom sheet opens ─────────────────────────
    LaunchedEffect(showMonturaSelector) {
        if (showMonturaSelector) {
            try {
                val activity = context as? androidx.activity.ComponentActivity
                if (activity != null) {
                    val entryPoint = EntryPointAccessors.fromActivity(
                        activity,
                        TryOnEntryPoint::class.java
                    )
                    val manager = entryPoint.getSessionManager()
                    val opticaId = manager.opticaId.first()
                    viewModel.loadMonturas(opticaId)
                }
            } catch (_: Exception) {
                // Fallback: load with empty opticaId
                viewModel.loadMonturas("")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prueba Virtual") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val currentState = state) {
                is TryOnState.Idle -> {
                    PhotoPickerSection(
                        onGalleryClick = {
                            if (hasPermission) {
                                galleryLauncher.launch("image/*")
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Se necesita permiso para acceder a la galería",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        },
                        onCameraClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Próximamente",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    )
                }

                is TryOnState.LoadingPhoto -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Cargando foto...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is TryOnState.PhotoSelected -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FacePreviewCanvas(state = currentState)

                        Spacer(modifier = Modifier.weight(1f))

                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        Text(
                            text = "Detectando rostro...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }
                }

                is TryOnState.FaceDetected -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        FacePreviewCanvas(state = currentState)

                        MeasurementDisplay(
                            measurements = currentState.measurements,
                            showPdwarning = false
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { showMonturaSelector = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Seleccionar Montura",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }

                is TryOnState.OverlayReady -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Composite image preview
                        FacePreviewCanvas(
                            state = currentState,
                            modifier = Modifier.weight(1f)
                        )

                        // Action buttons
                        ResultScreen(
                            state = currentState,
                            onSaveClick = {
                                val uri = viewModel.saveComposite()
                                scope.launch {
                                    val msg = if (uri != null) "Imagen guardada en galería"
                                              else "Error al guardar la imagen"
                                    snackbarHostState.showSnackbar(
                                        message = msg,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onShareClick = {
                                val uri = viewModel.getCompositeUri()
                                    ?: viewModel.saveComposite()
                                if (uri != null) {
                                    viewModel.shareComposite(uri)
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Primero guardá la imagen",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                            onSaveMeasurementsClick = {
                                viewModel.saveMeasurementsToEvaluacion(pacienteId)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Mediciones guardadas en la evaluación",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onRetryClick = { viewModel.resetState() }
                        )
                    }
                }

                is TryOnState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(onClick = { viewModel.resetState() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            // Montura selector bottom sheet
            if (showMonturaSelector) {
                MonturaSelectorBottomSheet(
                    monturas = monturas,
                    isLoading = monturasLoading,
                    onMonturaSelected = { montura ->
                        viewModel.selectMontura(montura)
                    },
                    onDismiss = { showMonturaSelector = false }
                )
            }
        }
    }
}

/**
 * Hilt entry point for accessing SessionManager and MonturaInventoryCoordinator
 * from a composable (non-ViewModel context).
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface TryOnEntryPoint {
    fun getMonturaInventoryCoordinator(): MonturaInventoryCoordinator
    fun getSessionManager(): SessionManager
}
