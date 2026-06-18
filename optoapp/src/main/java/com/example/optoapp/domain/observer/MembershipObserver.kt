package com.example.optoapp.domain.observer

import kotlinx.coroutines.flow.Flow

interface MembershipObserver {
    fun observeNewMembership(): Flow<Unit>
}
