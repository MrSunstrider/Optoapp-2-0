package com.example.optoapp.domain.sync

sealed class SyncResult {
    object Success : SyncResult()
    data class PartialSuccess(val uploaded: Int, val downloaded: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
    object NoConnection : SyncResult()
}
