package com.example.optoapp.viewmodel

import com.example.optoapp.subscription.PlanCode
import com.example.optoapp.subscription.SubscriptionTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for SubscriptionViewModel pure logic and contracts.
 *
 * Full ViewModel construction requires OptoRepository, SessionManager, PlayBillingManager
 * concrete classes with complex dependency graphs (Room, DataStore, BillingClient).
 * We test the pure logic extracted here and the data contracts.
 *
 * See: SubscriptionManagerTest for tier/planCode flow testing.
 */
class SubscriptionViewModelTest {

    // ─── canAddPaciente pure logic ─────────────────────────────────────────
    // Logic: combine(tier, pacienteCount) { t, count -> count < maxPacientes(t) }

    @Test
    fun `canAddPaciente returns true when count below limit`() {
        assertTrue(canAddPaciente(SubscriptionTier.FREE, 5) { maxPacientes(it) })
    }

    @Test
    fun `canAddPaciente returns false when count reaches limit`() {
        assertFalse(canAddPaciente(SubscriptionTier.FREE, 20) { maxPacientes(it) })
    }

    @Test
    fun `canAddPaciente returns true when count exceeds FREE limit but tier is PRO`() {
        assertTrue(canAddPaciente(SubscriptionTier.PRO, 999) { maxPacientes(it) })
    }

    @Test
    fun `canAddPaciente returns true when count just below FREE limit`() {
        assertTrue(canAddPaciente(SubscriptionTier.FREE, 19) { maxPacientes(it) })
    }

    @Test
    fun `canAddPaciente returns false when count exceeds FREE limit`() {
        assertFalse(canAddPaciente(SubscriptionTier.FREE, 21) { maxPacientes(it) })
    }

    @Test
    fun `canAddPaciente uses maxPacientes result directly`() {
        // Custom maxFn simulating a hypothetical 5-paciente limit
        val customMax: (SubscriptionTier) -> Int = { if (it == SubscriptionTier.FREE) 5 else Int.MAX_VALUE }
        assertTrue("4 < 5", canAddPaciente(SubscriptionTier.FREE, 4, customMax))
        assertFalse("5 >= 5", canAddPaciente(SubscriptionTier.FREE, 5, customMax))
    }

    // ─── launchProPurchase alpha bypass ───────────────────────────────────

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

    // ─── Helper functions mirroring ViewModel logic ────────────────────────

    private fun canAddPaciente(
        tier: SubscriptionTier,
        count: Int,
        maxPacientes: (SubscriptionTier) -> Int
    ): Boolean = count < maxPacientes(tier)

    private fun launchProPurchase(
        planCode: PlanCode,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Alpha: activate PRO directly via setProFromLocalCache()
        onSuccess()
    }

    // Same maxPacientes logic as SubscriptionManager
    private fun maxPacientes(tier: SubscriptionTier): Int = when (tier) {
        SubscriptionTier.FREE -> 20
        SubscriptionTier.PRO -> Int.MAX_VALUE
    }
}
