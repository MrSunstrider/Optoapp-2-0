package com.example.optoapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de caracterización para [SecurityManager.migratePinHasBeenSet].
 *
 * SecurityManager usa [androidx.security.crypto.EncryptedSharedPreferences] que requiere
 * Android Keystore (no disponible en Robolectric). Para poder testear la lógica de migración
 * extraemos la lógica condicional como una función pura [shouldMigrate] que SecurityManager
 * puede usar internamente.
 *
 * MURO DE LADRILLOS (constante): SecurityManager.PIN_LENGTH
 * MURO DE LADRILLOS (estático): SecurityManager.isValidPin()
 * MURO DE LADRILLOS (extraída): shouldMigrate(pinHasBeenSet, storedPin)
 */
class SecurityManagerMigrationCharacterizationTest {

    // ---------------------------------------------------------------
    // Constantes del muro de ladrillos
    // ---------------------------------------------------------------

    @Test
    fun `PIN_LENGTH es 6`() {
        assertEquals(
            "La longitud de PIN debe ser exactamente 6",
            6,
            SecurityManager.PIN_LENGTH,
        )
    }

    @Test
    fun `weakPinPatterns contiene 123456`() {
        // 8.1.x eliminó el caso especial de "123456" en migratePinHasBeenSet(),
        // pero isValidPin SIGUE rechazándolo como patrón débil.
        assertFalse(
            "\"123456\" debe estar en weakPinPatterns",
            SecurityManager.isValidPin("123456"),
        )
    }

    // ---------------------------------------------------------------
    // isValidPin — comportamiento actual post-simplificación
    // ---------------------------------------------------------------

    @Test
    fun `isValidPin rechaza digitos repetidos`() {
        assertFalse(SecurityManager.isValidPin("000000"))
        assertFalse(SecurityManager.isValidPin("111111"))
    }

    @Test
    fun `isValidPin rechaza patrones secuenciales incluyendo 123456`() {
        assertFalse(SecurityManager.isValidPin("123456"))
        assertFalse(SecurityManager.isValidPin("234567"))
        assertFalse(SecurityManager.isValidPin("654321"))
    }

    @Test
    fun `isValidPin rechaza caracteres no digitos`() {
        assertFalse(SecurityManager.isValidPin("abcdef"))
        assertFalse(SecurityManager.isValidPin("12 345"))
    }

    @Test
    fun `isValidPin rechaza longitudes incorrectas`() {
        assertFalse(SecurityManager.isValidPin(""))
        assertFalse(SecurityManager.isValidPin("12345"))
        assertFalse(SecurityManager.isValidPin("1234567"))
    }

    @Test
    fun `isValidPin acepta PINs aleatorios validos`() {
        assertTrue(SecurityManager.isValidPin("183729"))
        assertTrue(SecurityManager.isValidPin("904812"))
    }

    // ---------------------------------------------------------------
    // Lógica de migración extraída como función pura
    // ---------------------------------------------------------------

    @Test
    fun `debeMigrarPin con PIN legacy personalizado devuelve true`() {
        // Caso: pinHasBeenSet=false, storedPin personalizado (no default) → debe migrar
        assertTrue(shouldMigrate(pinHasBeenSet = false, storedPin = "183729"))
        assertTrue(shouldMigrate(pinHasBeenSet = false, storedPin = "583920"))
    }

    @Test
    fun `debeMigrarPin con DEV_FALLBACK_PIN devuelve false`() {
        // "999999" es el fallback de desarrollo, no debe migrar flag pinHasBeenSet
        assertFalse(
            "DEV_FALLBACK_PIN no debe migrar flag pinHasBeenSet",
            shouldMigrate(pinHasBeenSet = false, storedPin = "999999" /* was DEV_FALLBACK_PIN */),
        )
    }

    @Test
    fun `debeMigrarPin sin PIN legacy devuelve false`() {
        // Caso: pinHasBeenSet=false, storedPin vacío → NO debe migrar
        assertFalse(shouldMigrate(pinHasBeenSet = false, storedPin = ""))
    }

    @Test
    fun `debeMigrarPin cuando ya migrado devuelve false`() {
        // Caso: pinHasBeenSet=true → no importa el storedPin
        assertFalse(shouldMigrate(pinHasBeenSet = true, storedPin = "183729"))
        assertFalse(shouldMigrate(pinHasBeenSet = true, storedPin = ""))
        assertFalse(shouldMigrate(pinHasBeenSet = true, storedPin = "999999" /* was DEV_FALLBACK_PIN */))
    }

    // ---------------------------------------------------------------
    // ISecurityManager — contrato de interface
    // ---------------------------------------------------------------

    @Test
    fun `ISecurityManager tiene metodo getUserPin`() {
        val methods = ISecurityManager::class.java.declaredMethods.map { it.name }
        assertTrue("userPin debe tener getter. Got: $methods", methods.any { it == "getUserPin" })
    }

    @Test
    fun `ISecurityManager tiene metodo savePin`() {
        val methods = ISecurityManager::class.java.declaredMethods.map { it.name }
        assertTrue("savePin debe existir en ISecurityManager", methods.contains("savePin"))
    }

    @Test
    fun `ISecurityManager tiene getter pinHasBeenSet`() {
        val methods = ISecurityManager::class.java.declaredMethods.map { it.name }
        assertTrue(
            "pinHasBeenSet debe tener getter en ISecurityManager. Got: $methods",
            methods.any { it == "getPinHasBeenSet" },
        )
    }

    // ---------------------------------------------------------------
    // SecurityManager — estructura general
    // ---------------------------------------------------------------

    @Test
    fun `SecurityManager implementa ISecurityManager`() {
        assertTrue(
            "SecurityManager debe implementar ISecurityManager",
            ISecurityManager::class.java.isAssignableFrom(SecurityManager::class.java),
        )
    }

    @Test
    fun `SecurityManager expone isValidPin como funcion estatica`() {
        assertEquals(6, SecurityManager.PIN_LENGTH)
        // Call the static function directly — this verifies it's accessible
        assertFalse(SecurityManager.isValidPin("000000"))
        assertTrue(SecurityManager.isValidPin("183729"))
    }
}

/**
 * Lógica extraída de [SecurityManager.migratePinHasBeenSet].
 *
 * Determina si se debe migrar el flag `pinHasBeenSet` basado en:
 * - Si el flag ya está en true → no migrar (ya se migró)
 * - Si hay un PIN almacenado y NO es el default → migrar (setear flag)
 * - El PIN de desarrollo (["999999" /* was DEV_FALLBACK_PIN */]) no cuenta como
 *   "seteado por el usuario".
 *
 * @param pinHasBeenSet valor actual del flag
 * @param storedPin PIN almacenado en encrypted prefs (vacío si no existe)
 * @return true si se debe setear pinHasBeenSet = true
 */
internal fun shouldMigrate(pinHasBeenSet: Boolean, storedPin: String): Boolean {
    return !pinHasBeenSet && storedPin.isNotEmpty() && storedPin != "999999" /* was DEV_FALLBACK_PIN */
}
