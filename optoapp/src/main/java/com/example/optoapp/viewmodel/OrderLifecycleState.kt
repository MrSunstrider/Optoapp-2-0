package com.example.optoapp.viewmodel

import com.example.optoapp.data.AppRoles
import com.example.optoapp.domain.OrderStatusPolicy

data class OrderLifecycleState(
    val canCancel: Boolean = false,
    val canClaim: Boolean = false,
    val canHardDelete: Boolean = false,
    val isReadOnly: Boolean = false,
)

/** Terminal orders expose no action for any role; only privileged roles get actions on editable ones. */
fun orderLifecycleState(estado: String, role: String, hasTrace: Boolean): OrderLifecycleState {
    if (OrderStatusPolicy.isTerminal(estado)) return OrderLifecycleState(isReadOnly = true)
    if (!AppRoles.canDeleteRecords(role)) return OrderLifecycleState()
    return OrderLifecycleState(
        canCancel = OrderStatusPolicy.canCancel(estado),
        canClaim = OrderStatusPolicy.canClaim(estado),
        canHardDelete = !hasTrace,
    )
}
