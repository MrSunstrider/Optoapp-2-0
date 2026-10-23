package com.example.optoapp.domain.costo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LensCatalogRulesTest {

    @Test
    fun monofocal_lejos_listsResinaFamilyAndBlocksNearFotoFamiliesOnIntermedia() {
        val lejos = LensCatalogRules.familias(LensProductCatalog.TIPO_MONOFOCAL, LensProductCatalog.DIST_LEJOS)
        assertTrue(lejos.contains("Resina"))
        assertTrue(lejos.contains("Resina fotocromatica gris"))

        val intermedia = LensCatalogRules.familias(
            LensProductCatalog.TIPO_MONOFOCAL,
            LensProductCatalog.DIST_INTERMEDIA,
        )
        assertTrue(intermedia.contains("Resina"))
        assertFalse(intermedia.contains("Resina fotocromatica gris"))
        assertFalse(intermedia.contains("Cristal fotogray"))
    }

    @Test
    fun monofocal_intermedia_excludesPolarizadoTransitionFotoFromExtras() {
        val allowed = LensCatalogRules.tratamientosPermitidos(
            tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
            familia = "Resina",
            distancia = LensProductCatalog.DIST_CERCA,
        )
        assertTrue(allowed.contains(LensTreatments.UV_400))
        assertTrue(allowed.contains(LensTreatments.SOLAR))
        assertFalse(allowed.contains(LensTreatments.POLARIZADO))
        assertFalse(allowed.contains(LensTreatments.TRANSITION))
        assertFalse(allowed.contains(LensTreatments.FOTO_GRIS))
    }

    @Test
    fun monofocal_resina_stockSerie1_fromLowCylinder() {
        val resolution = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
                distancia = LensProductCatalog.DIST_LEJOS,
                familia = "Resina",
                tratamientos = listOf(LensTreatments.UV_400),
            ),
            LensRecetaInput(esfera = -2.0, cilindro = -1.25),
        )
        assertEquals(LensCatalogRules.STOCK, resolution.stockOFabricacion)
        assertEquals(1, resolution.serie)
        assertFalse(resolution.requiresManualIrCost)
    }

    @Test
    fun monofocal_resina_serie2_and_serie3_and_fabricacionPastSix() {
        val s2 = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
                distancia = LensProductCatalog.DIST_LEJOS,
                familia = "Resina",
            ),
            LensRecetaInput(esfera = -1.0, cilindro = -3.00),
        )
        assertEquals(LensCatalogRules.STOCK, s2.stockOFabricacion)
        assertEquals(2, s2.serie)

        val s3 = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
                distancia = LensProductCatalog.DIST_LEJOS,
                familia = "Resina",
            ),
            LensRecetaInput(esfera = -1.0, cilindro = -5.00),
        )
        assertEquals(3, s3.serie)

        val fab = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
                distancia = LensProductCatalog.DIST_LEJOS,
                familia = "Resina",
            ),
            LensRecetaInput(esfera = -7.0, cilindro = -1.0),
        )
        assertEquals(LensCatalogRules.FABRICACION, fab.stockOFabricacion)
        assertNull(fab.serie)
    }

    @Test
    fun monofocal_polarizado_forcesFabricacionEvenWhenSphereInStock() {
        val resolution = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
                distancia = LensProductCatalog.DIST_LEJOS,
                familia = "Resina",
                tratamientos = listOf(LensTreatments.POLARIZADO),
            ),
            LensRecetaInput(esfera = -1.0, cilindro = -0.5),
        )
        assertEquals(LensCatalogRules.FABRICACION, resolution.stockOFabricacion)
        assertNull(resolution.serie)
    }

    @Test
    fun forcesFabricacionIndependentOfRx_extrasFamilyAndCristalIr() {
        val solar = LensCatalogSelection(
            tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
            familia = "Resina",
            tratamientos = listOf(LensTreatments.SOLAR, LensTreatments.DISCROMATOPSIA),
        )
        assertTrue(LensCatalogRules.forcesFabricacionIndependentOfRx(solar))

        val uvOnly = LensCatalogSelection(
            tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
            familia = "Resina",
            tratamientos = listOf(LensTreatments.UV_400),
        )
        assertFalse(LensCatalogRules.forcesFabricacionIndependentOfRx(uvOnly))

        val alwaysFab = LensCatalogSelection(
            tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
            familia = "Resina antireflejo alto indice",
        )
        assertTrue(LensCatalogRules.forcesFabricacionIndependentOfRx(alwaysFab))

        val bifocalBlueAlwaysFab = LensCatalogSelection(
            tipoLente = LensProductCatalog.TIPO_BIFOCAL,
            subTipoBifocal = LensProductCatalog.SUB_FLAPTOP,
            familia = "Resina antireflejo Blue Def/Prot",
        )
        assertTrue(LensCatalogRules.forcesFabricacionIndependentOfRx(bifocalBlueAlwaysFab))

        val cristalNoIr = LensCatalogSelection(
            tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
            familia = "Cristal alto indice",
            indiceRefraccion = "1.8",
        )
        assertTrue(LensCatalogRules.forcesFabricacionIndependentOfRx(cristalNoIr))

        val cristal17 = LensCatalogSelection(
            tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
            familia = "Cristal alto indice",
            indiceRefraccion = "1.7",
        )
        assertFalse(LensCatalogRules.forcesFabricacionIndependentOfRx(cristal17))

        val plainResina = LensCatalogSelection(
            tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
            familia = "Resina",
        )
        assertFalse(LensCatalogRules.forcesFabricacionIndependentOfRx(plainResina))
    }

    @Test
    fun monofocal_solar_requiresCurvaBase_andForcesFab() {
        val resolution = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
                distancia = LensProductCatalog.DIST_LEJOS,
                familia = "Resina",
                tratamientos = listOf(LensTreatments.SOLAR),
            ),
            LensRecetaInput(esfera = -1.0, cilindro = 0.0),
        )
        assertTrue(resolution.requiresCurvaBase)
        assertEquals(LensCatalogRules.FABRICACION, resolution.stockOFabricacion)
    }

    @Test
    fun monofocal_altoIndice_requiresManualIr_alwaysFab() {
        val resolution = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
                distancia = LensProductCatalog.DIST_LEJOS,
                familia = "Resina antireflejo alto indice",
                indiceRefraccion = "1.6",
            ),
            LensRecetaInput(esfera = -1.0, cilindro = 0.0),
        )
        assertTrue(resolution.requiresManualIrCost)
        assertEquals(LensCatalogRules.FABRICACION, resolution.stockOFabricacion)
    }

    @Test
    fun cristal_altoIndice_stockOnlyWhenIr17() {
        val stock = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
                distancia = LensProductCatalog.DIST_LEJOS,
                familia = "Cristal alto indice",
                indiceRefraccion = "1.7",
            ),
            LensRecetaInput(esfera = -2.0, cilindro = -1.0),
        )
        assertEquals(LensCatalogRules.STOCK, stock.stockOFabricacion)
        assertEquals(1, stock.serie)

        val fab = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
                distancia = LensProductCatalog.DIST_LEJOS,
                familia = "Cristal alto indice",
                indiceRefraccion = "1.8",
            ),
            LensRecetaInput(esfera = -2.0, cilindro = -1.0),
        )
        assertEquals(LensCatalogRules.FABRICACION, fab.stockOFabricacion)
    }

    @Test
    fun discromatopsia_appendsTipoToMatrixTratamientoKey() {
        val resolution = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
                distancia = LensProductCatalog.DIST_LEJOS,
                familia = "Resina",
                tratamientos = listOf(LensTreatments.DISCROMATOPSIA),
                discromatopsiaTipo = "550 Rojo",
            ),
            LensRecetaInput(esfera = -1.0, cilindro = 0.0),
        )
        assertTrue(resolution.requiresDiscromatopsiaTipo)
        assertEquals("Filtro Discromatopsia (550 Rojo)", resolution.matrixTratamiento)
        assertEquals(LensCatalogRules.FABRICACION, resolution.stockOFabricacion)
    }

    @Test
    fun blueAlias_resolvesProtectToDefProtFamily() {
        val resolution = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
                distancia = LensProductCatalog.DIST_LEJOS,
                familia = "Resina antireflejo Blue Protect",
            ),
            LensRecetaInput(esfera = -1.0, cilindro = -0.75),
        )
        assertEquals("Resina antireflejo Blue Def/Prot", resolution.matrixFamilia)
        assertEquals(LensCatalogRules.STOCK, resolution.stockOFabricacion)
    }

    @Test
    fun bifocal_flaptop_resina_stockWindow_andAstigForcesFab_unlessOverride() {
        val stock = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_BIFOCAL,
                subTipoBifocal = LensProductCatalog.SUB_FLAPTOP,
                familia = "Resina",
            ),
            LensRecetaInput(esfera = 1.0, cilindro = 0.0, adicion = 2.0),
        )
        assertEquals(LensCatalogRules.STOCK, stock.stockOFabricacion)

        val withAstig = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_BIFOCAL,
                subTipoBifocal = LensProductCatalog.SUB_FLAPTOP,
                familia = "Resina",
            ),
            LensRecetaInput(esfera = 1.0, cilindro = -0.75, adicion = 2.0),
        )
        assertEquals(LensCatalogRules.FABRICACION, withAstig.stockOFabricacion)

        val forced = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_BIFOCAL,
                subTipoBifocal = LensProductCatalog.SUB_FLAPTOP,
                familia = "Resina",
                forzarStockIgnorandoAstigmatismo = true,
            ),
            LensRecetaInput(esfera = 1.0, cilindro = -0.75, adicion = 2.0),
        )
        assertEquals(LensCatalogRules.STOCK, forced.stockOFabricacion)
    }

    @Test
    fun bifocal_blueDef_alwaysFabricacion() {
        val resolution = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_BIFOCAL,
                subTipoBifocal = LensProductCatalog.SUB_FLAPTOP,
                familia = "Resina antireflejo Blue Def/Prot",
            ),
            LensRecetaInput(esfera = 2.0, cilindro = 0.0, adicion = 2.0),
        )
        assertEquals(LensCatalogRules.FABRICACION, resolution.stockOFabricacion)
        assertNull(resolution.serie)
    }

    @Test
    fun bifocal_invisible_cristalNotListed_flaptopCristalIsFab() {
        val invisibleFamilies = LensCatalogRules.familias(
            LensProductCatalog.TIPO_BIFOCAL,
            subTipoBifocal = LensProductCatalog.SUB_INVISIBLE,
        )
        assertFalse(invisibleFamilies.contains("Cristal"))

        val cristal = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_BIFOCAL,
                subTipoBifocal = LensProductCatalog.SUB_FLAPTOP,
                familia = "Cristal",
            ),
            LensRecetaInput(esfera = 1.0, cilindro = 0.0, adicion = 2.0),
        )
        assertEquals(LensCatalogRules.FABRICACION, cristal.stockOFabricacion)
        assertTrue(cristal.allowedTratamientos.contains(LensTreatments.FOTOGRAY))
    }

    @Test
    fun multifocal_and_ocupacional_policies() {
        val multiBlue = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_MULTIFOCAL,
                familia = "Resina antireflejo Blue Def/Prot",
            ),
            LensRecetaInput(esfera = 2.0, cilindro = 0.0, adicion = 2.0),
        )
        assertEquals(LensCatalogRules.FABRICACION, multiBlue.stockOFabricacion)

        val ocup = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_OCUPACIONAL,
                familia = "Resina",
                tratamientos = listOf(LensTreatments.UV_400),
            ),
            LensRecetaInput(esfera = 1.0, cilindro = 0.0),
        )
        assertEquals(LensCatalogRules.FABRICACION, ocup.stockOFabricacion)
        assertTrue(ocup.allowedTratamientos.contains(LensTreatments.BLUE_DEF_PROT))
    }

    @Test
    fun resolve_doesNotEmbedMonetaryCosts() {
        val resolution = LensCatalogRules.resolve(
            LensCatalogSelection(
                tipoLente = LensProductCatalog.TIPO_MONOFOCAL,
                distancia = LensProductCatalog.DIST_LEJOS,
                familia = "Resina",
            ),
            LensRecetaInput(esfera = -1.0, cilindro = -1.0),
        )
        // Series value only — cost is matrix responsibility
        assertEquals(1, resolution.serie)
        assertEquals("Resina", resolution.matrixFamilia)
    }
}
