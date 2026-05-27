package com.example.optoapp.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityManagerInstrumentedTest {

    private lateinit var context: Context
    private lateinit var securityManager: SecurityManager
    private val testPin = "481516"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear ESP before each test
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            "secure_security_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        encryptedPrefs.edit().clear().apply()
        // Clear DataStore
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs.clear()
            }
        }
        securityManager = SecurityManager(context)
    }

    @After
    fun tearDown() {
        runBlocking {
            context.dataStore.edit { it.clear() }
        }
    }

    @Test
    fun getStoredPin_returnsEmpty_whenFlagIsFalse() = runBlocking {
        // pinHasBeenSet defaults to false on fresh SecurityManager
        assertFalse(securityManager.pinHasBeenSet.first())
        assertEquals("", securityManager.getStoredPin())
    }

    @Test
    fun savePin_setsFlagAfterEspWrite() = runBlocking {
        val storedBefore = securityManager.getStoredPin()
        assertEquals("", storedBefore)

        securityManager.savePin(testPin)

        // After save, the pin should be retrievable
        assertEquals(testPin, securityManager.getStoredPin())
        assertTrue(securityManager.pinHasBeenSet.first())
    }

    @Test
    fun migration_autoSetsFlag_whenCustomPinExists() = runBlocking {
        // Pre-seed ESP with a custom PIN (simulating existing user)
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            "secure_security_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        encryptedPrefs.edit().putString("user_pin", "998877").apply()

        // Crear SecurityManager — migration se ejecuta lazy al leer pinHasBeenSet
        val sm = SecurityManager(context)

        // La migración corre como parte de first() — síncrono, sin race condition
        assertTrue("PIN legacy debe migrar flag a true", sm.pinHasBeenSet.first())
        assertEquals("998877", sm.getStoredPin())
    }

    @Test
    fun migration_doesNotSetFlag_whenDefaultPin() = runBlocking {
        // Pre-seed ESP with default "123456"
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            "secure_security_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        encryptedPrefs.edit().putString("user_pin", "123456").apply()

        // Crear SecurityManager — migration se ejecuta lazy al leer pinHasBeenSet
        val sm = SecurityManager(context)

        // El PIN default NO debe migrar el flag
        assertFalse("PIN default no debe migrar flag", sm.pinHasBeenSet.first())
        assertEquals("", sm.getStoredPin())
    }
}
