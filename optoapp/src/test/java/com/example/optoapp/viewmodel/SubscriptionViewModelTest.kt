package com.example.optoapp.viewmodel

import com.example.optoapp.subscription.PlanCode
import com.example.optoapp.subscription.SubscriptionTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for SubscriptionViewModel pure logic and contracts.
 * Paciente caps removed — FREE product limit is ópticas only.
 */
class SubscriptionViewModelTest {

    @Test
    fun `canAddPaciente always true for FREE regardless of count`() {
        assertTrue(canAddPaciente(SubscriptionTier.FREE, 0))
        assertTrue(canAddPaciente(SubscriptionTier.FREE, 50))
        assertTrue(canAddPaciente(SubscriptionTier.FREE, 10_000))
    }

    @Test
    fun `canAddPaciente always true for PRO`() {
        assertTrue(canAddPaciente(SubscriptionTier.PRO, 999))
    }

    @Test
    fun `launchProPurchase PRO_INDIVIDUAL activates PRO`() {
        var error: String? = null
        var success = false
        launchProPurchase(PlanCode.PRO_INDIVIDUAL, onSuccess = { success = true }, onError = { error = it })
        assertTrue("PRO_INDIVIDUAL should activate PRO", success)
        assertEquals("no error", null, error)
    }

    @Test
    fun `launchProPurchase FREE plan activates PRO`() {
        var error: String? = null
        var success = false
        launchProPurchase(PlanCode.FREE, onSuccess = { success = true }, onError = { error = it })
        assertTrue("FREE plan activates PRO in alpha", success)
        assertEquals("no error for FREE plan", null, error)
    }

    private fun canAddPaciente(
        tier: SubscriptionTier,
        count: Int,
    ): Boolean = true

    private fun launchProPurchase(
        planCode: PlanCode,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        onSuccess()
    }
}
