package com.jotape.di

import com.jotape.data.repository.AuthRepositoryImpl
import com.jotape.data.repository.InteractionRepositoryImpl
import com.jotape.domain.repository.AuthRepository
import com.jotape.domain.repository.InteractionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository dependencies.
 */
@Module
@InstallIn(SingletonComponent::class) // Or ViewModelComponent if you want repo scoped to ViewModel
abstract class RepositoryModule {

    /**
     * Binds the InteractionRepository interface to its implementation.
     * Hilt will know to provide InteractionRepositoryImpl when InteractionRepository is requested.
     */
    @Binds
    @Singleton // Ensure the repository is a singleton
    abstract fun bindInteractionRepository(
        interactionRepositoryImpl: InteractionRepositoryImpl
    ): InteractionRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

} 