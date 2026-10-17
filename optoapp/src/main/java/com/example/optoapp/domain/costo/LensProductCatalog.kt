package com.example.optoapp.domain.costo

/**
 * Product families for optical cost matrix keys.
 * [material] is the canonical string stored in costos_productos.material.
 */
data class ProductFamily(
    val material: String,
    val stockPolicy: StockPolicy,
    val extras: List<String>,
    val requiresManualIr: Boolean = false,
    /** When true, family itself is fotocromático / transition and blocked for Intermediate/Near. */
    val blockedForNearIntermediate: Boolean = false,
)

enum class StockPolicy {
    /** Monofocal: stock if |esf|≤6 and |cil|≤6; serie from cilindro. */
    MONOFOCAL_CIL_SERIES,

    /** Bifocal Flaptop Resina base: esf −1..+3, add +1..+3, no cyl (unless force stock). */
    BIFOCAL_FLAPTOP_RESINA,

    /** Bifocal Invisible / AR windows and Multifocal Resina/AR: esf +1..+3, add +1..+3. */
    BIFOCAL_PLUS_WINDOW,

    /** Always fabricacion (serie null). */
    ALWAYS_FABRICACION,

    /**
     * Cristal alto índice: stock only when IR is 1.7 and refraction fits monofocal stock;
     * otherwise fabricacion. Cost still often manual (IR).
     */
    CRISTAL_ALTO_INDICE_1_7_STOCK,
}

object LensProductCatalog {
    const val TIPO_MONOFOCAL = "Monofocal"
    const val TIPO_BIFOCAL = "Bifocal"
    const val TIPO_MULTIFOCAL = "Multifocal"
    const val TIPO_OCUPACIONAL = "Ocupacional"
    const val TIPO_LC = "Lentes de Contacto"

    const val DIST_LEJOS = "Lejos"
    const val DIST_INTERMEDIA = "Intermedia"
    const val DIST_CERCA = "Cerca"

    const val SUB_FLAPTOP = "Flaptop"
    const val SUB_INVISIBLE = "Invisible"

    val TIPOS: List<String> = listOf(
        TIPO_MONOFOCAL,
        TIPO_BIFOCAL,
        TIPO_MULTIFOCAL,
        TIPO_OCUPACIONAL,
        TIPO_LC,
    )

    val DISTANCIAS_MONOFOCAL: List<String> = listOf(DIST_LEJOS, DIST_INTERMEDIA, DIST_CERCA)

    val SUBTIPOS_BIFOCAL: List<String> = listOf(SUB_FLAPTOP, SUB_INVISIBLE)

    private val monoResinaExtrasBase = listOf(
        LensTreatments.UV_400,
        LensTreatments.SOLAR,
        LensTreatments.COLOREADO,
        LensTreatments.REDUCCION,
        LensTreatments.DISCROMATOPSIA,
        LensTreatments.POLARIZADO,
        LensTreatments.TRANSITION,
    )

    private val monoResinaAntirayasExtras = listOf(LensTreatments.REDUCCION)

    private val monoResinaArExtras = listOf(
        LensTreatments.UV_400,
        LensTreatments.REDUCCION,
        LensTreatments.DISCROMATOPSIA,
        LensTreatments.POLARIZADO,
    )

    private val monoResinaBlueExtras = listOf(
        LensTreatments.REDUCCION,
        LensTreatments.TRANSITION,
    )

    private val monoFotoExtras = listOf(LensTreatments.REDUCCION)

    private val monoPoliExtras = listOf(
        LensTreatments.COLOREADO,
        LensTreatments.REDUCCION,
        LensTreatments.DISCROMATOPSIA,
        LensTreatments.POLARIZADO,
        LensTreatments.TRANSITION,
        LensTreatments.FOTO_GRIS,
        LensTreatments.FOTO_BROWN,
    )

    private val monoPoliArExtras = listOf(
        LensTreatments.REDUCCION,
        LensTreatments.DISCROMATOPSIA,
        LensTreatments.POLARIZADO,
        LensTreatments.TRANSITION,
        LensTreatments.FOTO_GRIS,
        LensTreatments.FOTO_BROWN,
    )

    private val biResinaExtras = listOf(
        LensTreatments.UV_400,
        LensTreatments.ANTIRAYAS,
        LensTreatments.COLOREADO,
        LensTreatments.REDUCCION,
        LensTreatments.DISCROMATOPSIA,
        LensTreatments.POLARIZADO,
        LensTreatments.TRANSITION,
        LensTreatments.FOTO_GRIS,
        LensTreatments.FOTO_BROWN,
    )

    private val biArExtras = listOf(
        LensTreatments.AR_8,
        LensTreatments.AR_16,
        LensTreatments.REDUCCION,
        LensTreatments.DISCROMATOPSIA,
        LensTreatments.POLARIZADO,
        LensTreatments.TRANSITION,
        LensTreatments.FOTO_GRIS,
        LensTreatments.FOTO_BROWN,
    )

    private val multiResinaExtras = listOf(
        LensTreatments.UV_400,
        LensTreatments.AR_8,
        LensTreatments.AR_16,
        LensTreatments.BLUE_DEF_PROT,
        LensTreatments.ANTIRAYAS,
        LensTreatments.COLOREADO,
        LensTreatments.REDUCCION,
        LensTreatments.DISCROMATOPSIA,
        LensTreatments.POLARIZADO,
        LensTreatments.TRANSITION,
        LensTreatments.FOTO_GRIS,
        LensTreatments.FOTO_BROWN,
    )

    private val ocupExtras = listOf(
        LensTreatments.UV_400,
        LensTreatments.ANTIRAYAS,
        LensTreatments.ANTIREFLEJO,
        LensTreatments.BLUE_DEF_PROT,
        LensTreatments.REDUCCION,
    )

    val MONOFOCAL: List<ProductFamily> = listOf(
        ProductFamily("Resina", StockPolicy.MONOFOCAL_CIL_SERIES, monoResinaExtrasBase),
        ProductFamily("Resina antirayas", StockPolicy.MONOFOCAL_CIL_SERIES, monoResinaAntirayasExtras),
        ProductFamily("Resina antireflejo", StockPolicy.MONOFOCAL_CIL_SERIES, monoResinaArExtras),
        ProductFamily("Resina antireflejo Blue Def/Prot", StockPolicy.MONOFOCAL_CIL_SERIES, monoResinaBlueExtras),
        ProductFamily("Resina fotocromatica gris", StockPolicy.MONOFOCAL_CIL_SERIES, monoFotoExtras, blockedForNearIntermediate = true),
        ProductFamily("Resina fotocromatica brown", StockPolicy.MONOFOCAL_CIL_SERIES, monoFotoExtras, blockedForNearIntermediate = true),
        ProductFamily("Resina antireflejo fotocromatica gris", StockPolicy.MONOFOCAL_CIL_SERIES, monoFotoExtras, blockedForNearIntermediate = true),
        ProductFamily("Resina antireflejo fotocromatica brown", StockPolicy.MONOFOCAL_CIL_SERIES, monoFotoExtras, blockedForNearIntermediate = true),
        ProductFamily("Resina antireflejo Blue Def/Prot fotocromatica gris", StockPolicy.MONOFOCAL_CIL_SERIES, monoFotoExtras, blockedForNearIntermediate = true),
        ProductFamily("Resina antireflejo Blue Def/Prot fotocromatica brown", StockPolicy.MONOFOCAL_CIL_SERIES, monoFotoExtras, blockedForNearIntermediate = true),
        ProductFamily("Resina alto indice UV", StockPolicy.ALWAYS_FABRICACION, emptyList(), requiresManualIr = true),
        ProductFamily("Resina antireflejo alto indice", StockPolicy.ALWAYS_FABRICACION, emptyList(), requiresManualIr = true),
        ProductFamily("Resina antireflejo Blue Def/Prot alto indice", StockPolicy.ALWAYS_FABRICACION, emptyList(), requiresManualIr = true),
        ProductFamily(
            "Resina antireflejo Blue Def/Prot alto indice fotocromatico o transition",
            StockPolicy.ALWAYS_FABRICACION,
            emptyList(),
            requiresManualIr = true,
            blockedForNearIntermediate = true,
        ),
        ProductFamily("Cristal", StockPolicy.MONOFOCAL_CIL_SERIES, listOf(LensTreatments.REDUCCION)),
        ProductFamily("Cristal antireflejo", StockPolicy.MONOFOCAL_CIL_SERIES, listOf(LensTreatments.REDUCCION)),
        ProductFamily("Cristal fotogray", StockPolicy.MONOFOCAL_CIL_SERIES, listOf(LensTreatments.REDUCCION), blockedForNearIntermediate = true),
        ProductFamily("Cristal fotobrown", StockPolicy.MONOFOCAL_CIL_SERIES, listOf(LensTreatments.REDUCCION), blockedForNearIntermediate = true),
        ProductFamily("Cristal alto indice", StockPolicy.CRISTAL_ALTO_INDICE_1_7_STOCK, emptyList(), requiresManualIr = true),
        ProductFamily("Cristal alto indice antireflejo", StockPolicy.CRISTAL_ALTO_INDICE_1_7_STOCK, emptyList(), requiresManualIr = true),
        ProductFamily("Policarbonato", StockPolicy.MONOFOCAL_CIL_SERIES, monoPoliExtras),
        ProductFamily("Policarbonato antireflejo", StockPolicy.MONOFOCAL_CIL_SERIES, monoPoliArExtras),
        ProductFamily("Policarbonato antireflejo Blue Def/Prot", StockPolicy.MONOFOCAL_CIL_SERIES, monoPoliArExtras),
    )

    val BIFOCAL_FLAPTOP_RESINA: List<ProductFamily> = listOf(
        ProductFamily("Resina", StockPolicy.BIFOCAL_FLAPTOP_RESINA, biResinaExtras),
        ProductFamily("Resina antireflejo", StockPolicy.BIFOCAL_PLUS_WINDOW, biArExtras),
        ProductFamily("Resina antireflejo Blue Def/Prot", StockPolicy.ALWAYS_FABRICACION, biArExtras),
    )

    val BIFOCAL_INVISIBLE: List<ProductFamily> = listOf(
        ProductFamily("Resina", StockPolicy.BIFOCAL_PLUS_WINDOW, biResinaExtras),
        ProductFamily("Resina antireflejo", StockPolicy.BIFOCAL_PLUS_WINDOW, biArExtras),
        ProductFamily("Resina antireflejo Blue Def/Prot", StockPolicy.ALWAYS_FABRICACION, biArExtras),
    )

    val BIFOCAL_FLAPTOP_CRISTAL: List<ProductFamily> = listOf(
        ProductFamily(
            "Cristal",
            StockPolicy.ALWAYS_FABRICACION,
            listOf(
                LensTreatments.REDUCCION,
                LensTreatments.FOTOGRAY,
                LensTreatments.FOTOBROWN,
                LensTreatments.ANTIREFLEJO,
            ),
        ),
    )

    val BIFOCAL_FLAPTOP_POLI: List<ProductFamily> = listOf(
        ProductFamily(
            "Policarbonato",
            StockPolicy.ALWAYS_FABRICACION,
            listOf(
                LensTreatments.ANTIRAYAS,
                LensTreatments.AR_8,
                LensTreatments.AR_16,
                LensTreatments.REDUCCION,
                LensTreatments.DISCROMATOPSIA,
                LensTreatments.POLARIZADO,
                LensTreatments.TRANSITION,
                LensTreatments.FOTO_GRIS,
                LensTreatments.FOTO_BROWN,
            ),
        ),
    )

    val MULTIFOCAL: List<ProductFamily> = listOf(
        ProductFamily("Resina", StockPolicy.BIFOCAL_PLUS_WINDOW, multiResinaExtras),
        ProductFamily("Resina antireflejo", StockPolicy.BIFOCAL_PLUS_WINDOW, biArExtras),
        ProductFamily("Resina antireflejo Blue Def/Prot", StockPolicy.ALWAYS_FABRICACION, biArExtras),
        ProductFamily(
            "Cristal",
            StockPolicy.ALWAYS_FABRICACION,
            listOf(
                LensTreatments.REDUCCION,
                LensTreatments.FOTOGRAY,
                LensTreatments.FOTOBROWN,
                LensTreatments.AR_8,
                LensTreatments.AR_16,
            ),
        ),
        ProductFamily(
            "Policarbonato",
            StockPolicy.ALWAYS_FABRICACION,
            listOf(
                LensTreatments.ANTIRAYAS,
                LensTreatments.AR_8,
                LensTreatments.AR_16,
                LensTreatments.BLUE_DEF_PROT,
                LensTreatments.REDUCCION,
                LensTreatments.DISCROMATOPSIA,
                LensTreatments.POLARIZADO,
                LensTreatments.TRANSITION,
                LensTreatments.FOTO_GRIS,
                LensTreatments.FOTO_BROWN,
            ),
        ),
    )

    val OCUPACIONAL: List<ProductFamily> = listOf(
        ProductFamily("Resina", StockPolicy.ALWAYS_FABRICACION, ocupExtras),
        ProductFamily("Policarbonato", StockPolicy.ALWAYS_FABRICACION, ocupExtras),
    )

    fun families(
        tipoLente: String,
        distancia: String? = null,
        subTipoBifocal: String? = null,
    ): List<ProductFamily> {
        val nearOrInter = distancia == DIST_INTERMEDIA || distancia == DIST_CERCA
        val raw = when (tipoLente) {
            TIPO_MONOFOCAL -> MONOFOCAL
            TIPO_BIFOCAL -> when (subTipoBifocal) {
                SUB_INVISIBLE -> BIFOCAL_INVISIBLE
                SUB_FLAPTOP -> BIFOCAL_FLAPTOP_RESINA + BIFOCAL_FLAPTOP_CRISTAL + BIFOCAL_FLAPTOP_POLI
                else -> emptyList()
            }
            TIPO_MULTIFOCAL -> MULTIFOCAL
            TIPO_OCUPACIONAL -> OCUPACIONAL
            else -> emptyList()
        }
        return if (nearOrInter) raw.filterNot { it.blockedForNearIntermediate } else raw
    }

    fun findFamily(
        tipoLente: String,
        material: String,
        distancia: String? = null,
        subTipoBifocal: String? = null,
    ): ProductFamily? {
        val canonical = canonicalizeBlueAlias(material)
        return families(tipoLente, distancia, subTipoBifocal).find { canonicalizeBlueAlias(it.material) == canonical }
    }

    fun canonicalizeBlueAlias(name: String): String {
        if (name.contains("Blue Def/Prot", ignoreCase = true)) return name
        var normalized = name
        for (alias in listOf("Blue Defense", "Blue Protect", "Blue Prot", "Blue Def")) {
            normalized = normalized.replace(alias, "Blue Def/Prot", ignoreCase = true)
            if (normalized.contains("Blue Def/Prot", ignoreCase = true)) return normalized
        }
        return normalized
    }
}
