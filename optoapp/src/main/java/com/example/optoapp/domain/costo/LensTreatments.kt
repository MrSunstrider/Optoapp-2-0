package com.example.optoapp.domain.costo

/** Canonical extra-treatment labels used in catalog rules and matrix keys. */
object LensTreatments {
    const val UV_400 = "UV 400"
    const val SOLAR = "Solar"
    const val COLOREADO = "Coloreado"
    const val REDUCCION = "Reducción de diámetro"
    const val DISCROMATOPSIA = "Filtro Discromatopsia"
    const val POLARIZADO = "Polarizado"
    const val TRANSITION = "Transition"
    const val FOTO_GRIS = "Fotocromático gris"
    const val FOTO_BROWN = "Fotocromático brown"
    const val ANTIRAYAS = "Antirayas"
    const val ANTIREFLEJO = "Antireflejo"
    const val AR_8 = "Antireflejo 8 capas"
    const val AR_16 = "Antireflejo 16 capas"
    const val BLUE_DEF_PROT = "Antireflejo Blue Def/Prot"
    const val FOTOGRAY = "Fotogray"
    const val FOTOBROWN = "Fotobrown"

    /** Treatments that never keep a stock series once selected (UV 400 may remain stock). */
    val FORCE_FABRICACION: Set<String> = setOf(
        SOLAR,
        COLOREADO,
        REDUCCION,
        DISCROMATOPSIA,
        POLARIZADO,
        TRANSITION,
        FOTO_GRIS,
        FOTO_BROWN,
        ANTIRAYAS,
        ANTIREFLEJO,
        AR_8,
        AR_16,
        BLUE_DEF_PROT,
        FOTOGRAY,
        FOTOBROWN,
    )

    /** Blocked for monofocal Intermediate / Near distance. */
    val BLOCKED_NEAR_INTERMEDIATE: Set<String> = setOf(
        POLARIZADO,
        TRANSITION,
        FOTO_GRIS,
        FOTO_BROWN,
    )
}
