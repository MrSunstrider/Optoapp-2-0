package com.example.optoapp.data

data class SessionHealth(
    val hasValidSession: Boolean = false,
    val lastRefreshAtMs: Long = 0L,
    val lastRefreshSuccessful: Boolean = false,
    val lastRefreshError: String = "",
    val consecutiveRefreshFailures: Int = 0,
    val tokenExpiresAtMs: Long = 0L,
    val recentBackgroundErrors: List<BackgroundError> = emptyList(),
)

data class BackgroundError(
    val source: String,
    val message: String,
    val timestampMs: Long = System.currentTimeMillis(),
)
