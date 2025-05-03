package com.jotape.data.di

import android.content.Context
// Remover import do WorkManager
// import androidx.work.WorkManager
// Remover imports não utilizados de repositórios se não forem mais necessários aqui
// import com.jotape.data.repository.AuthRepositoryImpl
// import com.jotape.data.repository.InteractionRepositoryImpl
// import com.jotape.domain.repository.AuthRepository
// import com.jotape.domain.repository.InteractionRepository
// import dagger.Binds // Remover Binds não utilizado
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/*
// REMOVER ESTE MÓDULO DUPLICADO
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindInteractionRepository(impl: InteractionRepositoryImpl): InteractionRepository
}
*/

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Remover a função provideWorkManager
    /*
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
    */

    // Removed providePromptManager - Hilt will handle it via @Inject constructor
    // @Provides
    // @Singleton
    // fun providePromptManager(@ApplicationContext context: Context): PromptManager {
    //    return PromptManager(context)
    // }

    // Add other application-wide singleton providers here if needed
} 