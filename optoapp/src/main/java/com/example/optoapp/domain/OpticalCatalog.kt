package com.example.optoapp.domain

object OpticalCatalog {
    val MATERIALES = listOf("Resina", "Cristal", "Policarbonato", "Trivex")

    val TIPO_LENTE = listOf("Monofocal", "Bifocal", "Multifocal", "Ocupacional", "Lentes de Contacto")

    val TRATAMIENTOS = listOf(
        "UV 400",
        "Antirayas",
        "Antireflejo",
        "Antireflejo B Defense",
        "Fotocromático",
        "Polarizado",
        "Filtro Discromatopsia",
        "Coloreado",
        "Reducción de diámetro",
        "Alto Índice Rose 1.7",
        "Alto Índice Blanco 1.7",
        "Alto Índice Blanco 1.8",
        "Circadian",
    )

    val TIPO_ARO = mapOf(
        "Aro Completo" to "aro_completo",
        "Semi al aire" to "semi_aire",
        "Al aire" to "al_aire",
    )

    val SERIES = mapOf(
        "1ra serie (esféricos / cil 0 a -2.00)" to 1,
        "2da serie (cil -2.25 a -4.00)" to 2,
        "3ra serie (cil -4.25 a -6.00)" to 3,
        "Fabricación (sin serie)" to null,
    )
}
