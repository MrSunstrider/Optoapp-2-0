package com.example.optoapp.util

@Deprecated(
    "Migrated to com.example.optoapp.sync.rethrowIfCancellation. Use that import instead.",
    ReplaceWith("rethrowIfCancellation", "com.example.optoapp.sync.rethrowIfCancellation")
)
fun rethrowIfCancellation(e: Throwable) =
    com.example.optoapp.sync.rethrowIfCancellation(e)
