package com.example.optoapp.di

import com.example.optoapp.data.realtime.SupabaseObserver
import com.example.optoapp.domain.observer.MembershipObserver
import com.example.optoapp.domain.observer.TableObserver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ObserverModule {

    @Binds
    @Singleton
    abstract fun bindMembershipObserver(impl: SupabaseObserver): MembershipObserver

    @Binds
    @Singleton
    abstract fun bindTableObserver(impl: SupabaseObserver): TableObserver
}
