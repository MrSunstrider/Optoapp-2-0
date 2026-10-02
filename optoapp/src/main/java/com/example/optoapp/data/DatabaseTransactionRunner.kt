package com.example.optoapp.data

import androidx.room.withTransaction

/** Joins the caller's transaction when one is already open on this coroutine. */
interface DatabaseTransactionRunner {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}

class RoomTransactionRunner(private val database: OptoDatabase) : DatabaseTransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T = database.withTransaction(block)
}
