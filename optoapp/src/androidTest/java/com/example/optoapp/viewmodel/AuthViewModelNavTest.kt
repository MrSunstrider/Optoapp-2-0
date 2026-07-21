package com.example.optoapp.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.optoapp.data.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test: verifies that when [com.example.optoapp.data.SecurityManager.pinHasBeenSet]
 * is false (fresh install, no PIN configured), the ViewModel correctly exposes that state
 * so the navigation guard can redirect to [create_pin].
 */
@RunWith(AndroidJUnit4::class)
class AuthViewModelNavTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        runBlocking {
            context.dataStore.edit { it.clear() }
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            context.dataStore.edit { it.clear() }
        }
    }

    @Test
    fun pinHasBeenSet_isFalse_whenNoPinConfigured() = runBlocking {
        // Clear both ESP and DataStore to simulate fresh install
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_security_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ).edit().clear().apply()

        context.dataStore.edit { it.clear() }

        // The SessionManager reads the same DataStore key
        val sessionManager = com.example.optoapp.data.SessionManager(context)

        // Verify: on fresh install, pinHasBeenSet defaults to false
        assertFalse(sessionManager.pinHasBeenSet.first())
    }

    @Test
    fun pinHasBeenSet_isTrue_afterSettingPin() = runBlocking {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            "secure_security_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        encryptedPrefs.edit().clear().apply()
        context.dataStore.edit { it.clear() }

        val securityManager = com.example.optoapp.data.SecurityManager(context)
        securityManager.savePin("481516")

        val sessionManager = com.example.optoapp.data.SessionManager(context)

        // Verify: after saving a PIN, pinHasBeenSet is true
        assertTrue(sessionManager.pinHasBeenSet.first())
    }
}
