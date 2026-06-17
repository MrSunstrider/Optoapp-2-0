package com.example.optoapp.domain.observer

import kotlinx.coroutines.flow.Flow

interface TableObserver {
    fun observeTable(tableName: String, opticaId: String): Flow<Unit>
}
