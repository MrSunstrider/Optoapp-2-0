package com.example.optoapp.subscription

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.optoapp.data.FakeDataStore
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.membership.MembershipDataSource
import com.example.optoapp.data.membership.OpticaQueryHelper
import com.example.optoapp.data.membership.OpticaSettingsDataSource
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SubscriptionManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var fakeDataStore: FakeDataStore
    private lateinit var subscriptionManager: SubscriptionManager

    @Before
    fun setUp() {
        fakeDataStore = FakeDataStore()
        val dummySupabase = createSupabaseClient("https://test.supabase.co", "test-key") { }
        val dummyMembership = MembershipRepository(
            MembershipDataSource(dummySupabase, OpticaQueryHelper(dummySupabase)),
            OpticaSettingsDataSource(dummySupabase)
        )
        subscriptionManager = object : SubscriptionManager(
            context = RuntimeEnvironment.getApplication().applicationContext,
            membershipRepository = dummyMembership
        ) {
            override fun isDevProEffective(prefs: Preferences): Boolean = false
        }
        subscriptionManager.dataStore = fakeDataStore
    }

    // ─── maxPacientes (pure function) ──────────────────────────────────────

    @Test
    fun `maxPacientes FREE returns 50`() {
        assertEquals(50, subscriptionManager.maxPacientes(SubscriptionTier.FREE))
    }

    @Test
    fun `maxPacientes PRO returns Int MAX_VALUE`() {
        assertEquals(Int.MAX_VALUE, subscriptionManager.maxPacientes(SubscriptionTier.PRO))
    }

    // ─── maxOpticas (pure function) ────────────────────────────────────────

    @Test
    fun `maxOpticas FREE returns unlimited`() {
        assertNull(subscriptionManager.maxOpticas(PlanCode.FREE))
    }

    @Test
    fun `maxOpticas PRO_INDIVIDUAL returns 1`() {
        assertEquals(1, subscriptionManager.maxOpticas(PlanCode.PRO_INDIVIDUAL))
    }

    @Test
    fun `maxOpticas PRO_MULTISITE_15 returns 15`() {
        assertEquals(15, subscriptionManager.maxOpticas(PlanCode.PRO_MULTISITE_15))
    }

    @Test
    fun `maxOpticas ENTERPRISE returns null`() {
        assertNull(subscriptionManager.maxOpticas(PlanCode.ENTERPRISE))
    }

    @Test
    fun `maxOpticas DEV_OWNER returns null`() {
        assertNull(subscriptionManager.maxOpticas(PlanCode.DEV_OWNER))
    }

    // ─── planCode flow ─────────────────────────────────────────────────────

    @Test
    fun `planCode defaults to FREE`() = testScope.runTest {
        advanceUntilIdle()
        assertEquals(PlanCode.FREE, subscriptionManager.planCode.first())
    }

    @Test
    fun `planCode reflects cached plan`() = testScope.runTest {
        writePlanToDataStore("pro_individual")
        advanceUntilIdle()
        assertEquals(PlanCode.PRO_INDIVIDUAL, subscriptionManager.planCode.first())
    }

    @Test
    fun `planCode resolves PRO_MULTISITE_15`() = testScope.runTest {
        writePlanToDataStore("pro_multisite_15")
        advanceUntilIdle()
        assertEquals(PlanCode.PRO_MULTISITE_15, subscriptionManager.planCode.first())
    }

    @Test
    fun `planCode resolves unknown dev_owner to FREE`() = testScope.runTest {
        writePlanToDataStore("dev_owner")
        advanceUntilIdle()
        assertEquals(PlanCode.FREE, subscriptionManager.planCode.first())
    }

    @Test
    fun `planCode resolves ENTERPRISE`() = testScope.runTest {
        writePlanToDataStore("enterprise")
        advanceUntilIdle()
        assertEquals(PlanCode.ENTERPRISE, subscriptionManager.planCode.first())
    }

    // ─── tier flow ─────────────────────────────────────────────────────────

    @Test
    fun `tier defaults to FREE`() = testScope.runTest {
        advanceUntilIdle()
        assertEquals(SubscriptionTier.FREE, subscriptionManager.tier.first())
    }

    @Test
    fun `tier becomes PRO when cached plan is paid`() = testScope.runTest {
        writePlanToDataStore("pro_individual")
        advanceUntilIdle()
        assertEquals(SubscriptionTier.PRO, subscriptionManager.tier.first())
    }

    @Test
    fun `tier stays FREE for free plan`() = testScope.runTest {
        writePlanToDataStore("free")
        advanceUntilIdle()
        assertEquals(SubscriptionTier.FREE, subscriptionManager.tier.first())
    }

    @Test
    fun `tier stays FREE for unknown plan codes`() = testScope.runTest {
        writePlanToDataStore("unknown_plan")
        advanceUntilIdle()
        assertEquals(SubscriptionTier.FREE, subscriptionManager.tier.first())
    }

    @Test
    fun `tier becomes PRO when isDevProEffective returns true`() = testScope.runTest {
        val dummySupabase = createSupabaseClient("https://test.supabase.co", "test-key") { }
        val proManager = object : SubscriptionManager(
            context = RuntimeEnvironment.getApplication().applicationContext,
            membershipRepository = MembershipRepository(
                MembershipDataSource(dummySupabase, OpticaQueryHelper(dummySupabase)),
                OpticaSettingsDataSource(dummySupabase)
            )
        ) {
            override fun isDevProEffective(prefs: Preferences): Boolean = true
        }
        proManager.dataStore = fakeDataStore

        assertEquals(SubscriptionTier.PRO, proManager.tier.first())
    }

    // ─── setProFromLocalCache ──────────────────────────────────────────────

    @Test
    fun `setProFromLocalCache writes pro_individual`() = testScope.runTest {
        subscriptionManager.setProFromLocalCache()
        advanceUntilIdle()
        assertEquals(PlanCode.PRO_INDIVIDUAL, subscriptionManager.planCode.first())
    }

    // ─── refreshPlanFromServer ─────────────────────────────────────────────

    @Test
    fun `refreshPlanFromServer fetches and caches plan code`() = testScope.runTest {
        val ds = FakeDataStore()
        val dummySupabase2 = createSupabaseClient("https://test.supabase.co", "test-key") { }
        val manager = object : SubscriptionManager(
            context = RuntimeEnvironment.getApplication().applicationContext,
            membershipRepository = MembershipRepository(
                MembershipDataSource(dummySupabase2, OpticaQueryHelper(dummySupabase2)),
                OpticaSettingsDataSource(dummySupabase2)
            )
        ) {
            override fun isDevProEffective(prefs: Preferences): Boolean = false
            override suspend fun refreshPlanFromServer(opticaId: String) {
                dataStore.edit { it[stringPreferencesKey("sub_cached_plan")] = "enterprise" }
            }
        }
        manager.dataStore = ds

        manager.refreshPlanFromServer("optica-1")
        advanceUntilIdle()
        assertEquals(PlanCode.ENTERPRISE, manager.planCode.first())
    }

    @Test
    fun `refreshPlanFromServer does nothing when server returns null`() = testScope.runTest {
        val ds = FakeDataStore()
        ds.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                this[stringPreferencesKey("sub_cached_plan")] = "pro_individual"
            }
        }
        advanceUntilIdle()

        val dummySupabase3 = createSupabaseClient("https://test.supabase.co", "test-key") { }
        val manager = object : SubscriptionManager(
            context = RuntimeEnvironment.getApplication().applicationContext,
            membershipRepository = MembershipRepository(
                MembershipDataSource(dummySupabase3, OpticaQueryHelper(dummySupabase3)),
                OpticaSettingsDataSource(dummySupabase3)
            )
        ) {
            override fun isDevProEffective(prefs: Preferences): Boolean = false
            override suspend fun refreshPlanFromServer(opticaId: String) {
                // Do nothing — simulate server returning null
            }
        }
        manager.dataStore = ds

        manager.refreshPlanFromServer("optica-1")
        advanceUntilIdle()
        assertEquals("plan unchanged when server returns null",
            PlanCode.PRO_INDIVIDUAL, manager.planCode.first())
    }

    // ─── setDevProOverride ─────────────────────────────────────────────────

    @Test
    fun `setDevProOverride writes to DataStore`() = testScope.runTest {
        subscriptionManager.setDevProOverride(true)
        advanceUntilIdle()

        val written = fakeDataStore.data.first()[booleanPreferencesKey("sub_dev_pro")]
        assertTrue("dev_pro flag should be true", written == true)
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private suspend fun writePlanToDataStore(planCode: String) {
        fakeDataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                this[stringPreferencesKey("sub_cached_plan")] = planCode
            }
        }
    }
}
