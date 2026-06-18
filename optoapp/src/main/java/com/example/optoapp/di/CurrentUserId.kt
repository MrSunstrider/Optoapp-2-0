package com.example.optoapp.di

import javax.inject.Qualifier

/**
 * Qualifier for the current authenticated user identifier (email).
 * Provided in DatabaseModule from SessionManager.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrentUserId
