package com.example.optoapp.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
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
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val encryptedPrefs = EncryptedSharedPreferences.create(
            "secure_security_prefs",
            masterKeyAlias,
            context,
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
    fun tearDown() = runBlocking {
        context.dataStore.edit { it.clear() }
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
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val encryptedPrefs = EncryptedSharedPreferences.create(
            "secure_security_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        encryptedPrefs.edit().putString("user_pin", "998877").apply()

        // Create new SecurityManager — migration should auto-set the flag
        val sm = SecurityManager(context)

        // Give migration coroutine time to complete
        Thread.sleep(1000)

        assertTrue(sm.pinHasBeenSet.first())
        assertEquals("998877", sm.getStoredPin())
    }

    @Test
    fun migration_doesNotSetFlag_whenDefaultPin() = runBlocking {
        // Pre-seed ESP with default "123456"
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val encryptedPrefs = EncryptedSharedPreferences.create(
            "secure_security_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        encryptedPrefs.edit().putString("user_pin", "123456").apply()

        // Create new SecurityManager — migration should NOT set the flag
        val sm = SecurityManager(context)

        Thread.sleep(1000)

        assertFalse(sm.pinHasBeenSet.first())
        assertEquals("", sm.getStoredPin())
    }
}
