package com.example.optoapp.data

import com.example.optoapp.data.membership.MembershipDataSource
import com.example.optoapp.data.membership.OpticaSettingsDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class MembershipRepository @Inject constructor(
    private val membershipDataSource: MembershipDataSource,
    private val opticaSettingsDataSource: OpticaSettingsDataSource
) {
    suspend fun fetchMembershipsForCurrentUser(): List<OpticaMembership> =
        membershipDataSource.fetchMembershipsForCurrentUser()

    suspend fun fetchMembersForOptica(opticaId: String): List<OpticaMemberRow> =
        membershipDataSource.fetchMembersForOptica(opticaId)

    suspend fun assignRoleByEmail(opticaId: String, email: String, rol: String): Result<Unit> =
        membershipDataSource.assignRoleByEmail(opticaId, email, rol)

    suspend fun createOpticaForCurrentUser(
        nombreOptica: String,
        fiscalDocTipo: String = "",
        fiscalDocNumero: String = "",
        razonSocial: String = "",
        direccionFiscal: String = "",
        userId: String? = null
    ): Result<OpticaMembership> =
        opticaSettingsDataSource.createOpticaForCurrentUser(
            nombreOptica, fiscalDocTipo, fiscalDocNumero, razonSocial, direccionFiscal, userId
        )

    suspend fun fetchPlanSettings(opticaId: String): Result<PlanSettings> =
        opticaSettingsDataSource.fetchPlanSettings(opticaId)

    suspend fun updatePlanSettings(opticaId: String, settings: PlanSettings): Result<Unit> =
        opticaSettingsDataSource.updatePlanSettings(opticaId, settings)

    open suspend fun fetchOpticaPlan(opticaId: String): String? =
        opticaSettingsDataSource.fetchOpticaPlan(opticaId)

    suspend fun fetchOpticaLaboratorioSettings(opticaId: String): Pair<String, String>? =
        opticaSettingsDataSource.fetchOpticaLaboratorioSettings(opticaId)

    suspend fun fetchOpticaFiscalSettings(opticaId: String): OpticaFiscalSettings? =
        opticaSettingsDataSource.fetchOpticaFiscalSettings(opticaId)

    suspend fun fetchOpticaHeaderSummary(opticaId: String): OpticaHeaderSummary? =
        opticaSettingsDataSource.fetchOpticaHeaderSummary(opticaId)

    suspend fun updateOpticaFiscalSettings(
        opticaId: String,
        nombreComercial: String,
        docTipo: String,
        docNumero: String,
        razonSocial: String,
        direccionFiscal: String,
        distritoCiudadDepartamento: String,
        moneda: String,
        pais: String,
        contactoWhatsappTelefono: String
    ): Result<Unit> =
        opticaSettingsDataSource.updateOpticaFiscalSettings(
            opticaId, nombreComercial, docTipo, docNumero, razonSocial,
            direccionFiscal, distritoCiudadDepartamento, moneda, pais, contactoWhatsappTelefono
        )

    suspend fun updateOpticaLaboratorioSettings(
        opticaId: String,
        laboratorioNombre: String,
        laboratorioContacto: String
    ): Result<Unit> =
        opticaSettingsDataSource.updateOpticaLaboratorioSettings(opticaId, laboratorioNombre, laboratorioContacto)
}
