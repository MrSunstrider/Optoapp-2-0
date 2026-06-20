package com.example.optoapp.subscription

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.optoapp.BuildConfig
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class SubscriptionTier {
    FREE,
    PRO
}

enum class PlanCode {
    FREE,
    PRO_INDIVIDUAL,
    PRO_MULTISITE_15,
    ENTERPRISE,
    DEV_OWNER
}

@Singleton
open class SubscriptionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val membershipRepository: MembershipRepository
) {

    /** Override in tests to inject a fake DataStore. */
    @VisibleForTesting
    internal var dataStore: DataStore<Preferences> = context.dataStore

    private val keyDevPro = booleanPreferencesKey("sub_dev_pro")
    private val keyCachedPlan = stringPreferencesKey("sub_cached_plan")

    /** Solo true en builds de debug; en release nunca se expone el flag aunque quedara en DataStore. */
    val devProOverride: Flow<Boolean>
        get() = dataStore.data.map { isDevProEffective(it) }

    /**
     * Tier efectivo: override dev, plan en caché (post-sync o compra), o FREE.
     */
    val tier: Flow<SubscriptionTier>
        get() = combine(
            dataStore.data.map { isDevProEffective(it) },
            dataStore.data.map { (it[keyCachedPlan] ?: "free").lowercase().trim() }
        ) { dev, planStr ->
            when {
                dev -> SubscriptionTier.PRO
                toPlanCode(planStr) != PlanCode.FREE -> SubscriptionTier.PRO
                else -> SubscriptionTier.FREE
            }
        }

    val planCode: Flow<PlanCode>
        get() = dataStore.data.map {
            toPlanCode((it[keyCachedPlan] ?: "free").lowercase().trim())
        }

    open suspend fun refreshPlanFromServer(opticaId: String) {
        val plan = membershipRepository.fetchOpticaPlan(opticaId) ?: return
        dataStore.edit { prefs -> prefs[keyCachedPlan] = plan.lowercase().trim() }
    }

    fun maxPacientes(tier: SubscriptionTier): Int = when (tier) {
        SubscriptionTier.FREE -> FREE_MAX_PACIENTES
        SubscriptionTier.PRO -> Int.MAX_VALUE
    }

    fun maxOpticas(planCode: PlanCode): Int? = when (planCode) {
        PlanCode.FREE -> 1
        PlanCode.PRO_INDIVIDUAL -> 1
        PlanCode.PRO_MULTISITE_15 -> 15
        PlanCode.ENTERPRISE -> null
        PlanCode.DEV_OWNER -> null
    }

    suspend fun setDevProOverride(enabled: Boolean) {
        if (enabled && !BuildConfig.DEBUG) return
        dataStore.edit { prefs ->
            if (enabled) prefs[keyDevPro] = true else prefs.remove(keyDevPro)
        }
    }

    @VisibleForTesting
    internal open fun isDevProEffective(prefs: Preferences): Boolean {
        if (!BuildConfig.DEBUG) return false
        if (BuildConfig.FORCE_PRO_DEV) return true
        return prefs[keyDevPro] == true
    }

    /** Tras compra verificada en Play Billing (o prueba). */
    suspend fun setProFromLocalCache() {
        dataStore.edit { it[keyCachedPlan] = "pro_individual" }
    }

    private fun toPlanCode(raw: String): PlanCode = when (raw.lowercase().trim()) {
        "pro_multisite_15" -> PlanCode.PRO_MULTISITE_15
        "enterprise" -> PlanCode.ENTERPRISE
        "pro_individual", "pro", "paid", "premium" -> PlanCode.PRO_INDIVIDUAL
        else -> PlanCode.FREE
    }

    companion object {
        const val FREE_MAX_PACIENTES = 20
    }
}
