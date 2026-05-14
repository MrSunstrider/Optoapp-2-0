package com.example.optoapp.sync

import kotlinx.coroutines.sync.Mutex

/**
 * Testability interface for [SyncGate].
 * Allows faking the mutex in unit tests.
 */
interface ISyncGate {
    val mutex: Mutex
}
