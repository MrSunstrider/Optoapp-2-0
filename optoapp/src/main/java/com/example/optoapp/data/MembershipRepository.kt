package com.example.optoapp.data

import com.example.optoapp.data.membership.MembershipDataSource
import com.example.optoapp.data.membership.MembershipFetch
import com.example.optoapp.data.membership.OpticaSettingsDataSource
import com.example.optoapp.data.opticasettings.OpticaSettingsDao
import com.example.optoapp.data.opticasettings.OpticaSettingsEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class MembershipRepository @Inject constructor(
    private val membershipDataSource: MembershipDataSource,
    private val opticaSettingsDataSource: OpticaSettingsDataSource,
    private val opticaSettingsDao: OpticaSettingsDao,
) {
    suspend fun fetchMembershipsForCurrentUser(): MembershipFetch = membershipDataSource.fetchMembershipsForCurrentUser()

    suspend fun fetchMembersForOptica(opticaId: String): List<OpticaMemberRow> = membershipDataSource.fetchMembersForOptica(opticaId)

    suspend fun assignRoleByEmail(opticaId: String, email: String, rol: String): Result<Unit> = membershipDataSource.assignRoleByEmail(opticaId, email, rol)

    suspend fun createOpticaForCurrentUser(
        nombreOptica: String,
        fiscalDocTipo: String = "",
        fiscalDocNumero: String = "",
        razonSocial: String = "",
        direccionFiscal: String = "",
        userId: String? = null,
        overrideAccessToken: String? = null,
    ): Result<OpticaMembership> = opticaSettingsDataSource.createOpticaForCurrentUser(
        nombreOptica,
        fiscalDocTipo,
        fiscalDocNumero,
        razonSocial,
        direccionFiscal,
        userId,
        overrideAccessToken,
    )

    open suspend fun fetchOpticaPlan(opticaId: String): String? = opticaSettingsDataSource.fetchOpticaPlan(opticaId)

    suspend fun fetchOpticaLaboratorioSettings(opticaId: String): Pair<String, String>? = opticaSettingsDataSource.fetchOpticaLaboratorioSettings(opticaId)

    suspend fun fetchOpticaFiscalSettings(opticaId: String): OpticaFiscalSettings? = opticaSettingsDataSource.fetchOpticaFiscalSettings(opticaId)

    suspend fun fetchOpticaHeaderSummary(opticaId: String): OpticaHeaderSummary? = opticaSettingsDataSource.fetchOpticaHeaderSummary(opticaId)

    fun getOpticaSettingsFlow(opticaId: String): Flow<OpticaSettingsEntity?> = opticaSettingsDao.getByOpticaId(opticaId)

    suspend fun fetchOpticaSettings(opticaId: String): OpticaSettingsEntity? = opticaSettingsDataSource.fetchOpticaSettings(opticaId)

    suspend fun upsertOpticaSettings(settings: OpticaSettingsEntity) = opticaSettingsDao.upsert(settings)

    /** Fetch remote optica_settings and upsert into Room (no-op when remote missing/unauth). */
    open suspend fun syncOpticaSettingsFromRemote(opticaId: String) {
        if (opticaId.isBlank()) return
        val remote = opticaSettingsDataSource.fetchOpticaSettings(opticaId) ?: return
        opticaSettingsDao.upsert(remote)
    }

    open suspend fun upsertOpticaSettingsRemote(
        opticaId: String,
        configJson: String,
    ): Result<Unit> = opticaSettingsDataSource.upsertOpticaSettingsRemote(opticaId, configJson)

    suspend fun updateOpticaFiscalSettings(
        opticaId: String,
        nombreComercial: String,
        docTipo: String,
        docNumero: String,
        razonSocial: String,
        direccionFiscal: String,
    ): Result<Unit> = opticaSettingsDataSource.updateOpticaFiscalSettings(
        opticaId,
        nombreComercial,
        docTipo,
        docNumero,
        razonSocial,
        direccionFiscal,
    )

    suspend fun updateOpticaLaboratorioSettings(
        opticaId: String,
        laboratorioNombre: String,
        laboratorioContacto: String,
    ): Result<Unit> = opticaSettingsDataSource.updateOpticaLaboratorioSettings(opticaId, laboratorioNombre, laboratorioContacto)
}
