package com.example.optoapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Public DTOs (used across packages) ────────────────────────────────────────

@Serializable
data class OpticaMemberRow(
    @SerialName("optica_id") val opticaId: String,
    @SerialName("user_id") val userId: String,
    val email: String = "",
    val rol: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

data class PlanSettings(
    val planCode: String,
    val maxOpticas: Int?,
    val maxPacientesPorOptica: Int?,
    val maxUsuariosPorOptica: Int?,
    val planStatus: String
)

data class OpticaFiscalSettings(
    val nombreComercial: String = "",
    val docTipo: String = "",
    val docNumero: String = "",
    val razonSocial: String = "",
    val direccionFiscal: String = ""
)

data class OpticaHeaderSummary(
    val nombreOptica: String,
    val fiscalEtiqueta: String
)

// ── Internal DTOs (data package only) ─────────────────────────────────────────

@Serializable
internal data class UsuarioOpticaDto(
    @SerialName("user_id") val userId: String,
    @SerialName("optica_id") val opticaId: String,
    val rol: String = "admin"
)

@Serializable
internal data class UserProfileRow(
    @SerialName("user_id") val userId: String,
    val email: String
)

@Serializable
internal data class UsuarioOpticaUpsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("optica_id") val opticaId: String,
    val rol: String
)

@Serializable
internal data class OpticaDto(
    val id: String,
    val nombre: String = "",
    val plan: String = "free",
    @SerialName("plan_code") val planCode: String? = null,
    @SerialName("laboratorio_nombre") val laboratorioNombre: String = "",
    @SerialName("laboratorio_contacto") val laboratorioContacto: String = "",
    @SerialName("fiscal_doc_tipo") val fiscalDocTipo: String = "",
    @SerialName("fiscal_doc_numero") val fiscalDocNumero: String = "",
    @SerialName("razon_social") val razonSocial: String = "",
    @SerialName("direccion_fiscal") val direccionFiscal: String = ""
)

@Serializable
internal data class OpticaInsertDto(
    val id: String,
    val nombre: String,
    val plan: String = "free",
    @SerialName("plan_code") val planCode: String = "free",
    @SerialName("plan_source") val planSource: String = "manual",
    @SerialName("plan_status") val planStatus: String = "active",
    @SerialName("fiscal_doc_tipo") val fiscalDocTipo: String = "",
    @SerialName("fiscal_doc_numero") val fiscalDocNumero: String = "",
    @SerialName("razon_social") val razonSocial: String = "",
    @SerialName("direccion_fiscal") val direccionFiscal: String = ""
)

@Serializable
internal data class OpticaLaboratorioPatch(
    @SerialName("laboratorio_nombre") val laboratorioNombre: String,
    @SerialName("laboratorio_contacto") val laboratorioContacto: String
)

@Serializable
internal data class OpticaFiscalPatch(
    val nombre: String,
    @SerialName("fiscal_doc_tipo") val fiscalDocTipo: String,
    @SerialName("fiscal_doc_numero") val fiscalDocNumero: String,
    @SerialName("razon_social") val razonSocial: String,
    @SerialName("direccion_fiscal") val direccionFiscal: String
)
