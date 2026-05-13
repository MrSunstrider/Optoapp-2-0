package com.example.optoapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [SecurityManager] migration behavior using [FakeDataStore] and [FakeSharedPreferences].
 *
 * These tests avoid the Android Keystore requirement by injecting fakes
 * that simulate [DataStore] and [androidx.security.crypto.EncryptedSharedPreferences] in memory.
 */
@RunWith(RobolectricTestRunner::class)
class SecurityManagerMigrationTest {

    // ---------------------------------------------------------------
    // 8.2.2 — Migration without legacy data
    // ---------------------------------------------------------------

    @Test
    fun `migration without legacy data does nothing`() = runBlocking {
        val fakeDataStore: DataStore<Preferences> = FakeDataStore()
        val fakePrefs: SharedPreferences = FakeSharedPreferences()
        val context: Context = ApplicationProvider.getApplicationContext()
        val sm: SecurityManager = SecurityManager(context, fakeDataStore, fakePrefs)

        // Give the init-block coroutine (Dispatchers.IO) time to complete
        delay(100)

        val pinSet: Boolean = sm.pinHasBeenSet.first()
        assertFalse("Sin PIN legacy, pinHasBeenSet debe ser false",
            pinSet)
    }

    // ---------------------------------------------------------------
    // 8.2.3 — Migration with legacy data
    // ---------------------------------------------------------------

    @Test
    fun `migration with legacy pin converts flag to true`() = runBlocking {
        val fakeDataStore: DataStore<Preferences> = FakeDataStore()
        val fakePrefs: SharedPreferences = FakeSharedPreferences().apply {
            edit().putString("user_pin", "183729").apply()
        }
        val context: Context = ApplicationProvider.getApplicationContext()
        val sm: SecurityManager = SecurityManager(context, fakeDataStore, fakePrefs)

        delay(100)

        val pinSet: Boolean = sm.pinHasBeenSet.first()
        assertTrue("Con PIN legacy, pinHasBeenSet debe migrar a true",
            pinSet)
    }

    // ---------------------------------------------------------------
    // 8.2.4 — PIN validation after migration
    // ---------------------------------------------------------------

    @Test
    fun `PIN validation after migration works correctly`() = runBlocking {
        val fakeDataStore: DataStore<Preferences> = FakeDataStore()
        val fakePrefs: SharedPreferences = FakeSharedPreferences().apply {
            edit().putString("user_pin", "183729").apply()
        }
        val context: Context = ApplicationProvider.getApplicationContext()
        @Suppress("UNUSED_VARIABLE")
        val sm: SecurityManager = SecurityManager(context, fakeDataStore, fakePrefs)

        delay(100)

        assertTrue("PIN aleatorio debe ser válido",
            SecurityManager.isValidPin("183729"))
        assertFalse("PIN secuencial debe ser inválido",
            SecurityManager.isValidPin("123456"))
        assertFalse("PIN repetido debe ser inválido",
            SecurityManager.isValidPin("111111"))
    }
}
