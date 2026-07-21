package com.example.optoapp.data

import android.content.SharedPreferences

/**
 * In-memory [SharedPreferences] for unit tests.
 *
 * Simulates [EncryptedSharedPreferences] without Android Keystore.
 * All data is stored in a [HashMap] — no encryption, no disk I/O.
 * Supports [apply] and [commit] synchronously.
 */
internal class FakeSharedPreferences : SharedPreferences {

    private val map = mutableMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): Map<String, *> = map.toMap()
    override fun getString(key: String, defValue: String?): String? = (map[key] as? String) ?: defValue
    override fun getInt(key: String, defValue: Int): Int = (map[key] as? Int) ?: defValue
    override fun getLong(key: String, defValue: Long): Long = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = (map[key] as? Float) ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
    override fun getStringSet(key: String, defValue: Set<String>?): Set<String>? = (map[key] as? Set<*>)?.filterIsInstance<String>()?.toSet() ?: defValue
    override fun contains(key: String): Boolean = map.containsKey(key)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.remove(listener)
    }

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    private inner class FakeEditor : SharedPreferences.Editor {

        private val pending = mutableMapOf<String, Any?>()
        private val pendingRemovals = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { pending[key] = value }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply { pending[key] = value }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { pending[key] = value }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply { pending[key] = value }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply { pending[key] = value }

        override fun putStringSet(key: String, value: Set<String>?): SharedPreferences.Editor = apply { pending[key] = value }

        override fun remove(key: String): SharedPreferences.Editor = apply { pendingRemovals.add(key) }

        override fun clear(): SharedPreferences.Editor = apply { clearAll = true }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearAll) map.clear()
            pendingRemovals.forEach { map.remove(it) }
            pending.forEach { (key, value) ->
                if (value == null) map.remove(key) else map[key] = value
            }
            listeners.forEach { it.onSharedPreferenceChanged(this@FakeSharedPreferences, "") }
            pending.clear()
            pendingRemovals.clear()
            clearAll = false
        }
    }
}
