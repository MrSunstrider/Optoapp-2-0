package com.example.optoapp.ui.components.config

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optoapp.BuildConfig
import com.example.optoapp.R
import com.example.optoapp.subscription.PlanCode
import com.example.optoapp.ui.theme.OptoTokens
import com.example.optoapp.viewmodel.SubscriptionViewModel

@Composable
fun SubscriptionCard(
    planCode: PlanCode,
    devProOverride: Boolean,
    subscriptionVm: SubscriptionViewModel,
    context: Context
) {
    Card(
        shape = OptoTokens.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = OptoTokens.elevation.level1)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.config_subscription_section_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            val planLabel = when (planCode) {
                PlanCode.FREE -> "Free (máx. ${com.example.optoapp.subscription.SubscriptionManager.FREE_MAX_PACIENTES} pacientes)"
                PlanCode.PRO_INDIVIDUAL -> "Pro Individual (1 óptica)"
                PlanCode.PRO_MULTISITE_15 -> "Pro Multi-sede 15"
                PlanCode.ENTERPRISE -> "Enterprise"
                PlanCode.DEV_OWNER -> "Dev Owner (interno, ilimitado y exento de facturación)"
            }
            Text(stringResource(R.string.config_subscription_plan_label, planLabel), fontSize = 14.sp)
            if (BuildConfig.DEBUG && BuildConfig.FORCE_PRO_DEV) {
                Text(
                    stringResource(R.string.config_subscription_force_pro_debug),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (BuildConfig.DEBUG) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.config_subscription_dev_mode_title), fontSize = 14.sp)
                        Text(stringResource(R.string.config_subscription_dev_mode_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = devProOverride,
                        onCheckedChange = { subscriptionVm.setDevProOverride(it) }
                    )
                }
            }
            OutlinedButton(
                onClick = { subscriptionVm.refreshPlanFromServer() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.config_subscription_sync_plan_action))
            }
        }
    }
}
