package com.example.optoapp.di

import android.util.Log
import com.example.optoapp.domain.SyncLogger
import javax.inject.Inject

/**
 * Android implementation of [SyncLogger] that delegates to android.util.Log.
 * Provided as a POC — full migration of all domain android.util.Log usages is deferred.
 */
class AndroidSyncLogger @Inject constructor() : SyncLogger {
    override fun d(tag: String, msg: String) { Log.d(tag, msg) }
    override fun w(tag: String, msg: String, e: Throwable?) {
        if (e != null) Log.w(tag, msg, e) else Log.w(tag, msg)
    }
    override fun e(tag: String, msg: String, e: Throwable?) {
        if (e != null) Log.e(tag, msg, e) else Log.e(tag, msg)
    }
}
