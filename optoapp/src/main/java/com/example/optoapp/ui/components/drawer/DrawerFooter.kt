package com.example.optoapp.ui.components.drawer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.optoapp.ui.theme.LocalOptoDensity
import com.example.optoapp.viewmodel.SyncState

@Composable
fun DrawerFooter(
    syncState: SyncState,
    onSyncClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalOptoDensity.current
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = density.screenPadding))
        DrawerNavItem(
            label = if (syncState is SyncState.Loading) "Sincronizando..." else "Sincronizar Cloud",
            icon = Icons.Default.CloudSync,
            contentDescription = "Sincronizar",
            selected = false,
            isLoading = syncState is SyncState.Loading,
            onClick = onSyncClick,
        )
        DrawerNavItem(
            label = "Cerrar Sesión",
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = "Salir",
            selected = false,
            isDanger = true,
            onClick = onLogoutClick,
        )
    }
}
