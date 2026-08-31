package com.example.optoapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.optoapp.ui.theme.LocalOptoDensity

/**
 * Shell compartido de formularios: top bar, scroll, insets y bottom bar opcional.
 * Envuelve el patrón repetido en paciente, evaluación, dispensación, servicio e IF.
 */
@Composable
fun OptoFormShell(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onSave: (() -> Unit)? = null,
    isLoading: Boolean = false,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalOptoDensity.current

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            OptoTopAppBar(
                title = title,
                subtitle = subtitle,
                navigationIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") },
                onNavigationClick = onNavigateBack,
                actions = {
                    if (onSave != null) {
                        IconButton(onClick = onSave) {
                            Icon(Icons.Default.Check, contentDescription = "Guardar")
                        }
                    }
                },
            )
        },
        bottomBar = { bottomBar?.invoke() },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(density.screenPadding)
                // La bottomBar ya aplica navigationBarsPadding; aplicarlo aquí solo
                // cuando no hay bottomBar evita el doble inset (gap muerto sobre los botones).
                .then(if (bottomBar == null) Modifier.navigationBarsPadding() else Modifier)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(density.sectionGap),
            content = content,
        )
    }
}
