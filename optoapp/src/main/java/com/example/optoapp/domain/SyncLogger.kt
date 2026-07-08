package com.example.optoapp.domain

/**
 * Abstraction over platform logging so domain-layer code does not depend
 * on android.util.Log directly.
 *
 * Full migration of all 19 domain files is deferred — this is a POC used
 * by [NetworkRetryHelper] and expanded as needed.
 */
interface SyncLogger {
    fun d(tag: String, msg: String)
    fun w(tag: String, msg: String, e: Throwable? = null)
    fun e(tag: String, msg: String, e: Throwable? = null)
}
