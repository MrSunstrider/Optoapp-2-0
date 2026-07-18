package com.example.optoapp.util

/**
 * Pure-Kotlin logger that breaks the domain layer's dependency on android.util.Log.
 * In debug builds this writes to stdout/stderr; in production it can be swapped for
 * a structured logger (e.g. Timber, SLF4J) without touching domain code.
 */
object AppLogger {
    fun d(tag: String, msg: String) = println("DEBUG/$tag: $msg")
    fun e(tag: String, msg: String, t: Throwable? = null) {
        System.err.println("ERROR/$tag: $msg")
        t?.printStackTrace()
    }
    fun w(tag: String, msg: String, t: Throwable? = null) {
        println("WARN/$tag: $msg")
        t?.printStackTrace()
    }
    fun i(tag: String, msg: String) = println("INFO/$tag: $msg")
    fun v(tag: String, msg: String) = println("VERBOSE/$tag: $msg")
}
