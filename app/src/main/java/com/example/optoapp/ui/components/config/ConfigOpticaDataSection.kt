package com.example.optoapp.ui.components.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.R
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoTextField
import com.example.optoapp.viewmodel.FiscalConfigUi

data class FiscalDraftUpdate(
    val nombreComercial: String? = null,
    val docTipo: String? = null,
    val docNumero: String? = null,
    val razonSocial: String? = null,
    val direccionFiscal: String? = null,
    val distritoCiudadDepartamento: String? = null,
    val moneda: String? = null,
    val pais: String? = null,
    val contactoWhatsappTelefono: String? = null
)

@Composable
fun FiscalDataSection(
    fiscalUi: FiscalConfigUi,
    onDraftChange: (FiscalDraftUpdate) -> Unit,
    onSave: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.config_fiscal_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.config_fiscal_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OptoTextField(
                value = fiscalUi.nombreComercial,
                onValueChange = { onDraftChange(FiscalDraftUpdate(nombreComercial = it)) },
                label = stringResource(R.string.config_fiscal_nombre_comercial_label)
            )
            DropdownField(
                label = stringResource(R.string.config_fiscal_tipo_doc_label),
                selected = fiscalUi.docTipo,
                options = listOf("RUC", "RUS"),
                onSelected = { onDraftChange(FiscalDraftUpdate(docTipo = it)) }
            )
            OptoTextField(
                value = fiscalUi.docNumero,
                onValueChange = { onDraftChange(FiscalDraftUpdate(docNumero = it)) },
                label = stringResource(R.string.config_fiscal_numero_doc_label, fiscalUi.docTipo)
            )
            OptoTextField(
                value = fiscalUi.razonSocial,
                onValueChange = { onDraftChange(FiscalDraftUpdate(razonSocial = it)) },
                label = stringResource(R.string.config_fiscal_razon_social_label)
            )
            OptoTextField(
                value = fiscalUi.direccionFiscal,
                onValueChange = { onDraftChange(FiscalDraftUpdate(direccionFiscal = it)) },
                label = stringResource(R.string.config_fiscal_direccion_label)
            )
            OptoTextField(
                value = fiscalUi.distritoCiudadDepartamento,
                onValueChange = { onDraftChange(FiscalDraftUpdate(distritoCiudadDepartamento = it)) },
                label = stringResource(R.string.config_fiscal_distrito_label)
            )
            OptoTextField(
                value = fiscalUi.moneda,
                onValueChange = { onDraftChange(FiscalDraftUpdate(moneda = it)) },
                label = stringResource(R.string.config_fiscal_moneda_label)
            )
            OptoTextField(
                value = fiscalUi.pais,
                onValueChange = { onDraftChange(FiscalDraftUpdate(pais = it)) },
                label = stringResource(R.string.config_fiscal_pais_label)
            )
            OptoTextField(
                value = fiscalUi.contactoWhatsappTelefono,
                onValueChange = { onDraftChange(FiscalDraftUpdate(contactoWhatsappTelefono = it)) },
                label = stringResource(R.string.config_fiscal_contacto_label),
                keyboardType = KeyboardType.Phone
            )
            Button(
                onClick = onSave,
                enabled = !fiscalUi.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (fiscalUi.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.config_save_fiscal))
                }
            }
            fiscalUi.message?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp) }
            fiscalUi.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        }
    }
}

@Composable
fun ClinicalIntegritySection(
    onResolveDuplicates: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.config_clinical_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.config_clinical_section_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onResolveDuplicates,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.config_clinical_resolve_ho_action))
            }
        }
    }
}

@Composable
fun UsuariosRolesSection(
    roleUi: com.example.optoapp.viewmodel.RoleManagementUiState,
    allowedRoles: List<String>,
    canAssignAdminRole: Boolean,
    onEmailChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onAssignRole: () -> Unit,
    onRefresh: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.config_users_roles_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.config_users_roles_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OptoTextField(
                value = roleUi.emailInput,
                onValueChange = onEmailChange,
                label = stringResource(R.string.config_users_roles_email_label)
            )
            DropdownField(
                label = stringResource(R.string.config_users_roles_role_label),
                selected = roleUi.roleInput,
                options = allowedRoles,
                onSelected = onRoleChange
            )
            if (!canAssignAdminRole) {
                Text(
                    stringResource(R.string.config_users_roles_admin_only),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAssignRole,
                    enabled = !roleUi.loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.config_users_roles_assign_action)) }
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !roleUi.loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.config_users_roles_refresh_action)) }
            }
            roleUi.message?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp) }
            roleUi.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            if (roleUi.members.isEmpty()) {
                Text(stringResource(R.string.config_users_roles_empty), fontSize = 12.sp)
            } else {
                roleUi.members.take(20).forEach { row ->
                    Text("• ${row.email.ifBlank { row.userId }} — ${row.rol}", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SucursalesSection(
    nuevaSucursalNombre: String,
    creatingSucursal: Boolean,
    onNombreChange: (String) -> Unit,
    onCreate: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.config_branches_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.config_branches_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OptoTextField(
                value = nuevaSucursalNombre,
                onValueChange = onNombreChange,
                label = stringResource(R.string.config_branches_name_label)
            )
            Button(
                onClick = onCreate,
                enabled = nuevaSucursalNombre.trim().isNotEmpty() && !creatingSucursal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (creatingSucursal) {
                        stringResource(R.string.config_branches_creating_action)
                    } else {
                        stringResource(R.string.config_branches_create_action)
                    }
                )
            }
        }
    }
}

@Composable
fun PlanManagementSection(
    planUi: com.example.optoapp.viewmodel.PlanManagementUiState,
    canUseInternalPlan: Boolean,
    onPlanCodeChange: (String) -> Unit,
    onPlanStatusChange: (String) -> Unit,
    onMaxOpticasChange: (String) -> Unit,
    onMaxPacientesChange: (String) -> Unit,
    onMaxUsuariosChange: (String) -> Unit,
    onApplyPreset: () -> Unit,
    onSave: () -> Unit,
    onReload: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.config_plan_management_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.config_plan_management_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            DropdownField(
                label = stringResource(R.string.config_plan_label),
                selected = planUi.planCode,
                options = if (canUseInternalPlan) {
                    listOf("free", "pro_individual", "pro_multisite_15", "enterprise", "dev_owner")
                } else {
                    listOf("free", "pro_individual", "pro_multisite_15", "enterprise")
                },
                onSelected = onPlanCodeChange
            )
            DropdownField(
                label = stringResource(R.string.config_plan_status_label),
                selected = planUi.planStatus,
                options = listOf("active", "grace", "canceled"),
                onSelected = onPlanStatusChange
            )
            OptoTextField(
                value = planUi.maxOpticasInput,
                onValueChange = onMaxOpticasChange,
                label = stringResource(R.string.config_plan_max_opticas_label),
                keyboardType = KeyboardType.Number
            )
            OptoTextField(
                value = planUi.maxPacientesInput,
                onValueChange = onMaxPacientesChange,
                label = stringResource(R.string.config_plan_max_pacientes_label),
                keyboardType = KeyboardType.Number
            )
            OptoTextField(
                value = planUi.maxUsuariosInput,
                onValueChange = onMaxUsuariosChange,
                label = stringResource(R.string.config_plan_max_usuarios_label),
                keyboardType = KeyboardType.Number
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onApplyPreset,
                        enabled = !planUi.loading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.config_plan_apply_preset), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = onSave,
                        enabled = !planUi.loading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.config_plan_save_action), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                OutlinedButton(
                    onClick = onReload,
                    enabled = !planUi.loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.config_plan_reload))
                }
            }
            planUi.message?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp) }
            planUi.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        }
    }
}
